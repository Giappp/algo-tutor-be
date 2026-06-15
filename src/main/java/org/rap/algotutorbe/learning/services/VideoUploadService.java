package org.rap.algotutorbe.learning.services;

import lombok.RequiredArgsConstructor;
import org.rap.algotutorbe.common.errors.ErrorCode;
import org.rap.algotutorbe.common.exception.AppException;
import org.rap.algotutorbe.common.services.S3Service;
import org.rap.algotutorbe.learning.dto.video.*;
import org.rap.algotutorbe.learning.enums.VideoProcessingStatus;
import org.rap.algotutorbe.learning.models.VideoLesson;
import org.rap.algotutorbe.learning.repositories.LessonRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

import java.util.*;

@Service
@RequiredArgsConstructor
public class VideoUploadService {
    private final LessonService lessonService;
    private final LessonRepository lessonRepository;
    private final S3Service s3Service;

    @Value("${app.video.max-file-size-bytes:2147483648}")
    private long maxFileSizeBytes;

    @Value("${app.video.multipart-part-size-bytes:10485760}")
    private long partSizeBytes;

    @Value("#{'${app.video.allowed-types:video/mp4,video/quicktime}'.split(',')}")
    private Set<String> allowedTypes;

    @Transactional
    public VideoUploadInitiateResponse initiate(Long lessonId, VideoUploadInitiateRequest request) {
        VideoLesson video = getVideoLesson(lessonId);
        validateUpload(request);

        String extension = extensionOf(request.fileName());
        String objectKey = "lessons/%d/video/source/%s%s".formatted(
                lessonId,
                UUID.randomUUID(),
                extension
        );

        var upload = s3Service.createMultipartUpload(objectKey, request.contentType());
        video.setSourceObjectKey(objectKey);
        video.setFileSizeBytes(request.fileSize());
        video.setMimeType(request.contentType());
        video.setProcessingStatus(VideoProcessingStatus.UPLOADING);
        lessonRepository.save(video);

        int totalParts = Math.toIntExact((request.fileSize() + partSizeBytes - 1) / partSizeBytes);
        return new VideoUploadInitiateResponse(upload.uploadId(), objectKey, partSizeBytes, totalParts);
    }

    public List<VideoUploadPartResponse> createPartUrls(Long lessonId, VideoUploadPartsRequest request) {
        VideoLesson video = getVideoLesson(lessonId);
        validateObjectKey(video, request.objectKey());
        int totalParts = totalParts(video);
        if (request.partNumbers().stream().anyMatch(part -> part > totalParts)) {
            throw new AppException(ErrorCode.VIDEO_UPLOAD_INCOMPLETE);
        }

        return request.partNumbers().stream()
                .distinct()
                .sorted()
                .map(partNumber -> new VideoUploadPartResponse(
                        partNumber,
                        s3Service.createPresignedUploadPartUrl(request.objectKey(), request.uploadId(), partNumber)
                ))
                .toList();
    }

    @Transactional
    public VideoUploadCompleteResponse complete(Long lessonId, VideoUploadCompleteRequest request) {
        VideoLesson video = getVideoLesson(lessonId);
        validateObjectKey(video, request.objectKey());

        List<CompletedPart> parts = request.parts().stream()
                .sorted(Comparator.comparing(VideoUploadCompleteRequest.CompletedVideoPart::partNumber))
                .map(part -> CompletedPart.builder()
                        .partNumber(part.partNumber())
                        .eTag(part.eTag())
                        .build())
                .toList();
        if (parts.size() != totalParts(video)
                || parts.stream().map(CompletedPart::partNumber).distinct().count() != parts.size()) {
            throw new AppException(ErrorCode.VIDEO_UPLOAD_INCOMPLETE);
        }

        s3Service.completeMultipartUpload(request.objectKey(), request.uploadId(), parts);
        HeadObjectResponse object = s3Service.headObject(request.objectKey());

        if (video.getFileSizeBytes() != null && !Objects.equals(object.contentLength(), video.getFileSizeBytes())) {
            video.setProcessingStatus(VideoProcessingStatus.FAILED);
            lessonRepository.save(video);
            throw new AppException(ErrorCode.VIDEO_UPLOAD_INCOMPLETE);
        }

        video.setDurationSeconds(request.durationSeconds());
        video.setFileSizeBytes(object.contentLength());
        video.setMimeType(object.contentType());
        video.setProcessingStatus(VideoProcessingStatus.READY);
        lessonRepository.save(video);

        return toCompleteResponse(video);
    }

    @Transactional
    public void abort(Long lessonId, VideoUploadAbortRequest request) {
        VideoLesson video = getVideoLesson(lessonId);
        validateObjectKey(video, request.objectKey());
        s3Service.abortMultipartUpload(request.objectKey(), request.uploadId());
        video.setProcessingStatus(VideoProcessingStatus.PENDING_UPLOAD);
        video.setSourceObjectKey(null);
        video.setFileSizeBytes(null);
        video.setMimeType(null);
        lessonRepository.save(video);
    }

    private void validateUpload(VideoUploadInitiateRequest request) {
        if (!allowedTypes.contains(request.contentType())) {
            throw new AppException(ErrorCode.VIDEO_INVALID_TYPE);
        }
        if (request.fileSize() <= 0 || request.fileSize() > maxFileSizeBytes) {
            throw new AppException(ErrorCode.VIDEO_INVALID_SIZE);
        }
    }

    private VideoLesson getVideoLesson(Long lessonId) {
        var lesson = lessonService.getOrThrow(lessonId);
        if (!(lesson instanceof VideoLesson video)) {
            throw new AppException(ErrorCode.VIDEO_LESSON_REQUIRED);
        }
        return video;
    }

    private void validateObjectKey(VideoLesson video, String objectKey) {
        if (!objectKey.equals(video.getSourceObjectKey())
                || !objectKey.startsWith("lessons/%d/video/source/".formatted(video.getId()))) {
            throw new AppException(ErrorCode.VIDEO_UPLOAD_NOT_FOUND);
        }
    }

    private String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0) {
            return "";
        }
        return fileName.substring(dot).replaceAll("[^a-zA-Z0-9.]", "");
    }

    private int totalParts(VideoLesson video) {
        if (video.getFileSizeBytes() == null || video.getFileSizeBytes() <= 0) {
            throw new AppException(ErrorCode.VIDEO_UPLOAD_INCOMPLETE);
        }
        return Math.toIntExact((video.getFileSizeBytes() + partSizeBytes - 1) / partSizeBytes);
    }

    private VideoUploadCompleteResponse toCompleteResponse(VideoLesson video) {
        return new VideoUploadCompleteResponse(
                video.getId(),
                video.getSourceObjectKey(),
                video.getFileSizeBytes(),
                video.getMimeType(),
                video.getDurationSeconds(),
                video.getProcessingStatus()
        );
    }
}

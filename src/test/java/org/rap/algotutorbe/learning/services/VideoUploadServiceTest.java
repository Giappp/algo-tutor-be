package org.rap.algotutorbe.learning.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rap.algotutorbe.common.errors.ErrorCode;
import org.rap.algotutorbe.common.exception.AppException;
import org.rap.algotutorbe.common.services.S3Service;
import org.rap.algotutorbe.learning.dto.video.VideoUploadCompleteRequest;
import org.rap.algotutorbe.learning.dto.video.VideoUploadInitiateRequest;
import org.rap.algotutorbe.learning.enums.VideoProcessingStatus;
import org.rap.algotutorbe.learning.models.VideoLesson;
import org.rap.algotutorbe.learning.repositories.LessonRepository;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VideoUploadServiceTest {
    @Mock
    private LessonService lessonService;
    @Mock
    private LessonRepository lessonRepository;
    @Mock
    private S3Service s3Service;

    private VideoUploadService videoUploadService;
    private VideoLesson video;

    @BeforeEach
    void setUp() {
        videoUploadService = new VideoUploadService(lessonService, lessonRepository, s3Service);
        ReflectionTestUtils.setField(videoUploadService, "maxFileSizeBytes", 100L);
        ReflectionTestUtils.setField(videoUploadService, "partSizeBytes", 10L);
        ReflectionTestUtils.setField(videoUploadService, "allowedTypes", Set.of("video/mp4"));

        video = new VideoLesson();
        video.setId(42L);
        when(lessonService.getOrThrow(42L)).thenReturn(video);
    }

    @Test
    void initiate_shouldCreateMultipartUploadAndMarkVideoUploading() {
        when(s3Service.createMultipartUpload(any(), any()))
                .thenReturn(CreateMultipartUploadResponse.builder().uploadId("upload-1").build());

        var response = videoUploadService.initiate(
                42L,
                new VideoUploadInitiateRequest("lesson.mp4", "video/mp4", 25L)
        );

        assertThat(response.uploadId()).isEqualTo("upload-1");
        assertThat(response.totalParts()).isEqualTo(3);
        assertThat(response.objectKey()).startsWith("lessons/42/video/source/").endsWith(".mp4");
        assertThat(video.getProcessingStatus()).isEqualTo(VideoProcessingStatus.UPLOADING);
        verify(lessonRepository).save(video);
    }

    @Test
    void initiate_shouldRejectUnsupportedContentType() {
        assertThatThrownBy(() -> videoUploadService.initiate(
                42L,
                new VideoUploadInitiateRequest("lesson.avi", "video/avi", 25L)
        ))
                .isInstanceOfSatisfying(AppException.class,
                        exception -> assertThat(exception.getError()).isEqualTo(ErrorCode.VIDEO_INVALID_TYPE));
    }

    @Test
    void complete_shouldVerifyObjectAndMarkVideoReady() {
        video.setSourceObjectKey("lessons/42/video/source/video.mp4");
        video.setFileSizeBytes(20L);
        video.setMimeType("video/mp4");
        video.setProcessingStatus(VideoProcessingStatus.UPLOADING);
        when(s3Service.headObject(video.getSourceObjectKey()))
                .thenReturn(HeadObjectResponse.builder()
                        .contentLength(20L)
                        .contentType("video/mp4")
                        .build());

        var response = videoUploadService.complete(
                42L,
                new VideoUploadCompleteRequest(
                        "upload-1",
                        video.getSourceObjectKey(),
                        60,
                        List.of(
                                new VideoUploadCompleteRequest.CompletedVideoPart(1, "etag-1"),
                                new VideoUploadCompleteRequest.CompletedVideoPart(2, "etag-2")
                        )
                )
        );

        assertThat(response.processingStatus()).isEqualTo(VideoProcessingStatus.READY);
        assertThat(video.getDurationSeconds()).isEqualTo(60);
        verify(s3Service).completeMultipartUpload(any(), any(), any());
        verify(lessonRepository).save(video);
    }

    @Test
    void complete_shouldRejectMissingPartsBeforeCallingS3() {
        video.setSourceObjectKey("lessons/42/video/source/video.mp4");
        video.setFileSizeBytes(20L);

        assertThatThrownBy(() -> videoUploadService.complete(
                42L,
                new VideoUploadCompleteRequest(
                        "upload-1",
                        video.getSourceObjectKey(),
                        60,
                        List.of(new VideoUploadCompleteRequest.CompletedVideoPart(1, "etag-1"))
                )
        ))
                .isInstanceOfSatisfying(AppException.class,
                        exception -> assertThat(exception.getError()).isEqualTo(ErrorCode.VIDEO_UPLOAD_INCOMPLETE));
    }
}

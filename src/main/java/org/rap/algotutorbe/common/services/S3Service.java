package org.rap.algotutorbe.common.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rap.algotutorbe.common.api.ApiResponse;
import org.rap.algotutorbe.common.api.FileUploadResponse;
import org.rap.algotutorbe.common.api.PresignedUploadTarget;
import org.rap.algotutorbe.common.errors.ErrorCode;
import org.rap.algotutorbe.common.exception.AppException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3Service {
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${aws.bucket.name}")
    private String bucketName;

    @Value("${aws.region}")
    private String region;

    @Value("${app.judge.storage-path}")
    private String localStoragePath;

    @Value("#{'${app.file.allowed-image-types}'.split(',')}")
    private List<String> allowedImageTypes;

    public ListBucketsResponse testAwsConnection() {
        return s3Client.listBuckets();
    }

    public ApiResponse<FileUploadResponse> uploadImage(MultipartFile file) {
        validateImage(file);
        String objectKey = generateObjectKey(file);
        PutObjectResponse putObjectResponse = uploadToS3(file, objectKey);
        String fileUrl = buildFileUrl(objectKey);
        FileUploadResponse response = new FileUploadResponse(fileUrl, objectKey, putObjectResponse.size(), file.getContentType());
        return ApiResponse.buildSuccess(response);
    }

    public PresignedUploadTarget createPresignedUploadTarget(
            Long lessonId,
            String testCaseUuid,
            String fileName
    ) {
        String objectKey = buildTestCaseObjectKey(lessonId, testCaseUuid, fileName);

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .contentType("application/octet-stream")
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(5))
                .putObjectRequest(putObjectRequest)
                .build();

        String uploadUrl = s3Presigner.presignPutObject(presignRequest)
                .url()
                .toString();

        String fileUrl = buildFileUrl(objectKey);

        return new PresignedUploadTarget(objectKey, uploadUrl, fileUrl);
    }

    public String buildTestCaseObjectKey(Long lessonId, String testCaseUuid, String fileName) {
        String cleanFileName = sanitizeFileName(fileName);

        return String.format(
                "lessons/%d/testcases/%s/%s",
                lessonId,
                testCaseUuid,
                cleanFileName
        );
    }

    public String buildFileUrl(String objectKey) {
        return String.format(
                "https://%s.s3.%s.amazonaws.com/%s",
                bucketName,
                region,
                objectKey
        );
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "file";
        }

        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    public Path getOrCreateLocalTestCaseFile(Long lessonId, String s3Url) {
        String s3Key = extractS3KeyFromUrl(s3Url);

        String[] parts = s3Key.split("/");
        if (parts.length < 4) {
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        String fileName = sanitizeFileName(parts[parts.length - 1]);
        String testCaseUuid = parts[parts.length - 2];

        Path baseDir = Paths.get(localStoragePath)
                .toAbsolutePath()
                .normalize();

        Path localFilePath = baseDir
                .resolve(lessonId.toString())
                .resolve(testCaseUuid)
                .resolve(fileName)
                .normalize();

        if (!localFilePath.startsWith(baseDir)) {
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        if (Files.exists(localFilePath)) {
            log.info("Cache hit: {}", localFilePath);
            return localFilePath;
        }

        try {
            Files.createDirectories(localFilePath.getParent());

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            s3Client.getObject(getObjectRequest, localFilePath);

            return localFilePath;
        } catch (IOException e) {
            log.error("Cannot create local testcase file: {}", localFilePath, e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, e);
        } catch (Exception e) {
            log.error("Cannot download testcase from S3. key={}", s3Key, e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, e);
        }
    }

    private String extractS3KeyFromUrl(String s3Url) {
        String marker = ".amazonaws.com/";

        int index = s3Url.indexOf(marker);
        if (index < 0) {
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        return s3Url.substring(index + marker.length());
    }

    public Path downloadFileToTempDisk(String s3Url, String prefix) {
        String s3Key = s3Url.substring(s3Url.indexOf(".amazonaws.com/") + 15);
        String suffix = s3Url.endsWith(".out") ? ".out" : ".in";

        try {
            // 1. Tạo một file trống trong thư mục tạm của OS (Ví dụ: /tmp/input_4829348.in)
            Path tempFile = Files.createTempFile(prefix + "_", suffix);

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            s3Client.getObject(getObjectRequest, tempFile);

            return tempFile;
        } catch (Exception e) {
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, e);
        }
    }

    public String getDisplayUrl(Long lessonId, String testCaseUuid, String fileName) {
        String objectKey = buildTestCaseObjectKey(lessonId, testCaseUuid, fileName);
        return buildFileUrl(objectKey);
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.FILE_EMPTY);
        }
        if (!allowedImageTypes.contains(file.getContentType())) {
            throw new AppException(ErrorCode.IMAGE_TYPE_ERROR);
        }
    }

    private String generateObjectKey(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String cleanFilename = (originalFilename != null) ? originalFilename.replaceAll("[^a-zA-Z0-9.-]", "_") : "image";
        return "images/" + UUID.randomUUID() + "-" + cleanFilename;
    }

    private PutObjectResponse uploadToS3(MultipartFile file, String objectKey) {
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .contentType(file.getContentType())
                    .build();

            return s3Client.putObject(
                    putObjectRequest,
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );
        } catch (IOException e) {
            throw new AppException(ErrorCode.S3_UNEXPECTED, e);
        } catch (Exception e) {
            throw new AppException(ErrorCode.S3_CONNECTION, e);
        }
    }
}

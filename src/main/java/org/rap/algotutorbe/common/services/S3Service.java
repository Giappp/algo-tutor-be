package org.rap.algotutorbe.common.services;

import lombok.RequiredArgsConstructor;
import org.rap.algotutorbe.common.api.ApiResponse;
import org.rap.algotutorbe.common.api.FileUploadResponse;
import org.rap.algotutorbe.common.errors.ErrorCode;
import org.rap.algotutorbe.common.exception.AppException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3Service {
    private final S3Client s3Client;

    @Value("${aws.bucket.name}")
    private String bucketName;

    @Value("${aws.region}")
    private String region;

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

    private String buildFileUrl(String objectKey) {
        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, objectKey);
    }
}

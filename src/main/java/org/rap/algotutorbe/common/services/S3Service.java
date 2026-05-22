package org.rap.algotutorbe.common.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rap.algotutorbe.common.api.ApiResponse;
import org.rap.algotutorbe.common.api.FileUploadResponse;
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

    public String generatePresignedUploadUrl(Long problemId, String testCaseUuid, String fileName) {
        String s3Key = String.format("problems/%d/testcases/%s//%s", problemId, testCaseUuid, fileName);

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(5))
                .putObjectRequest(putObjectRequest)
                .build();

        return s3Presigner.presignPutObject(presignRequest).url().toString();
    }

    /**
     * Lấy file testcase từ local cache, nếu không có sẽ tải từ S3 về đĩa.
     *
     * @param problemId ID của bài tập (để phân thư mục gọn gàng)
     * @param s3Url     Link đầy đủ của file lưu trong DB (VD: https://.../input.in)
     * @return Path dẫn tới file vật lý trên ổ đĩa Server
     */
    public Path getOrCreateLocalTestCaseFile(Long problemId, String s3Url) {
        // 1. Trích xuất S3 Key từ URL
        // Ví dụ: "problems/12/testcases/uuid-abc/input.in"
        String s3Key = s3Url.substring(s3Url.indexOf(".amazonaws.com/") + 15);

        // Tách lấy tên file gốc (input.in hoặc output.out) từ S3 Key
        String fileName = s3Key.substring(s3Key.lastIndexOf("/") + 1);

        // Lấy chuỗi định danh độc nhất của bộ testcase (UUID) từ cấu trúc thư mục S3
        String[] parts = s3Key.split("/");
        String testCaseUuid = parts[parts.length - 2];

        Path localFilePath = Paths.get(localStoragePath, problemId.toString(), testCaseUuid, fileName);

        // 3. Nếu file đã tồn tại cục bộ, trả về luôn (Hit Cache)
        if (Files.exists(localFilePath)) {
            log.info("Cache Hit: File đã tồn tại cục bộ tại -> {}", localFilePath);
            return localFilePath;
        }

        // 4. Nếu chưa tồn tại (Miss Cache), tiến hành tạo thư mục cha và tải từ S3 về
        try {
            Files.createDirectories(localFilePath.getParent());
            log.info("Cache Miss: Đang tải file từ S3 về ổ đĩa -> {}", localFilePath);

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            // Stream trực tiếp từ S3 ghi vào ổ cứng thông qua SDK v2
            s3Client.getObject(getObjectRequest, localFilePath);

            return localFilePath;
        } catch (IOException e) {
            log.error("Lỗi khi tạo thư mục hoặc ghi file cục bộ: {}", e.getMessage());
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, e);
        } catch (Exception e) {
            log.error("Lỗi kết nối hoặc tải dữ liệu từ AWS S3: {}", e.getMessage());
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, e);
        }
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

    /**
     * Tạo đường dẫn công khai (hoặc private bản chuẩn) của file sau khi upload thành công
     */
    public String getDisplayUrl(Long problemId, String testCaseUuid, String fileName) {
        return String.format("https://%s.s3.%s.amazonaws.com/problems/%d/testcases/%s/%s",
                bucketName, region, problemId, testCaseUuid, fileName);
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

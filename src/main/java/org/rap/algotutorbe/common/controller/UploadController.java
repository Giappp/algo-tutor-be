package org.rap.algotutorbe.common.controller;

import lombok.RequiredArgsConstructor;
import org.rap.algotutorbe.common.api.ApiResponse;
import org.rap.algotutorbe.common.api.FileUploadResponse;
import org.rap.algotutorbe.common.services.S3Service;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;

@RestController
@RequestMapping("/upload")
@RequiredArgsConstructor
public class UploadController {
    private final S3Service s3Service;

    @GetMapping("/test")
    public ApiResponse<ListBucketsResponse> testAwsConnection() {
        return ApiResponse.buildSuccess(s3Service.testAwsConnection());
    }

    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<FileUploadResponse>> uploadImage(MultipartFile file) {
        return ResponseEntity.ok(s3Service.uploadImage(file));
    }
}

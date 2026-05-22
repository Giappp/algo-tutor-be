package org.rap.algotutorbe.learning.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.rap.algotutorbe.common.api.ApiResponse;
import org.rap.algotutorbe.common.services.S3Service;
import org.rap.algotutorbe.learning.dto.testcase.SaveTestCaseRequest;
import org.rap.algotutorbe.learning.dto.testcase.TestCaseDTO;
import org.rap.algotutorbe.learning.dto.testcase.TestCasePresignRequest;
import org.rap.algotutorbe.learning.dto.testcase.TestCasePresignResponse;
import org.rap.algotutorbe.learning.services.TestCaseService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/testcases")
@RequiredArgsConstructor
public class TestCaseController {
    private final TestCaseService testCaseService;
    private final S3Service s3Service;

    @PostMapping("/lessons/{lessonId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TestCaseDTO>> createTestCase(
            @PathVariable Long lessonId,
            @RequestBody @Valid SaveTestCaseRequest request) {
        return ResponseEntity.ok(testCaseService.create(lessonId, request));
    }

    @GetMapping("/lessons/{lessonId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<TestCaseDTO>>> getTestCasesByLesson(@PathVariable Long lessonId) {
        return ResponseEntity.ok(testCaseService.getByLessonId(lessonId));
    }

    @PostMapping("/lessons/{lessonId}/presigned-url")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<TestCasePresignResponse>>> getPresignedUrl(@PathVariable Long lessonId,
                                                                                      @RequestBody TestCasePresignRequest request) {
        List<TestCasePresignResponse> responses = new ArrayList<>();
        String testCaseUuid = UUID.randomUUID().toString();

        for (TestCasePresignRequest.FileInfo fileInfo : request.files()) {
            String uploadUrl = s3Service.generatePresignedUploadUrl(lessonId, testCaseUuid, fileInfo.fileName());
            String downloadUrl = s3Service.getDisplayUrl(lessonId, testCaseUuid, fileInfo.fileName());

            responses.add(new TestCasePresignResponse(fileInfo.fileName(),
                    fileInfo.fileType(),
                    uploadUrl,
                    downloadUrl));
        }
        return ResponseEntity.ok(ApiResponse.buildSuccess(responses));
    }
}

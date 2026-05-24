package org.rap.algotutorbe.learning.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.rap.algotutorbe.common.api.ApiResponse;
import org.rap.algotutorbe.learning.dto.testcase.TestCasePresignResponse;
import org.rap.algotutorbe.learning.dto.testcase.TestCasePresignedUploadRequest;
import org.rap.algotutorbe.learning.services.TestCaseFileUploadService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/lessons/{lessonId}/testcases")
@RequiredArgsConstructor
public class TestCaseUploadController {

    private final TestCaseFileUploadService testCaseFileUploadService;

    @PostMapping("/presigned-url")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<ApiResponse<List<TestCasePresignResponse>>> createPresignedUploadUrls(
            @PathVariable Long lessonId,
            @Valid @RequestBody TestCasePresignedUploadRequest request
    ) {
        var response = testCaseFileUploadService.createPresignedUploadUrls(lessonId, request);
        return ResponseEntity.ok(
                ApiResponse.buildSuccess(response)
        );
    }
}
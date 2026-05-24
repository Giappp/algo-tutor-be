package org.rap.algotutorbe.learning.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.rap.algotutorbe.common.api.ApiResponse;
import org.rap.algotutorbe.common.services.S3Service;
import org.rap.algotutorbe.learning.dto.testcase.SaveTestCaseRequest;
import org.rap.algotutorbe.learning.dto.testcase.TestCaseDTO;
import org.rap.algotutorbe.learning.services.TestCaseService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
}

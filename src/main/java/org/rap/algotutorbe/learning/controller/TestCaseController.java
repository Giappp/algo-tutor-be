package org.rap.algotutorbe.learning.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.rap.algotutorbe.common.api.ApiResponse;
import org.rap.algotutorbe.learning.dto.TestCaseDTO;
import org.rap.algotutorbe.learning.services.TestCaseService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/testcases")
@RequiredArgsConstructor
public class TestCaseController {
    private final TestCaseService testCaseService;

    @PostMapping("/lessons/{lessonId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Object>> createTestCase(
            @PathVariable Long lessonId,
            @RequestBody @Valid TestCaseDTO request) {
        return ResponseEntity.ok(testCaseService.create(lessonId, request));
    }

    @GetMapping("/lessons/{lessonId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Object>> getTestCasesByLesson(@PathVariable Long lessonId) {
        return ResponseEntity.ok(testCaseService.getByLessonId(lessonId));
    }

    @PutMapping("/{testCaseId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Object>> updateTestCase(
            @PathVariable Long testCaseId,
            @RequestBody @Valid TestCaseDTO request) {
        return ResponseEntity.ok(testCaseService.update(testCaseId, request));
    }

    @DeleteMapping("/{testCaseId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Object>> deleteTestCase(@PathVariable Long testCaseId) {
        return ResponseEntity.ok(testCaseService.delete(testCaseId));
    }
}

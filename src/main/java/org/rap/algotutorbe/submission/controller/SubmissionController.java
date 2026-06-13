package org.rap.algotutorbe.submission.controller;

import lombok.RequiredArgsConstructor;
import org.rap.algotutorbe.common.api.ApiResponse;
import org.rap.algotutorbe.common.api.PageResponse;
import org.rap.algotutorbe.submission.dto.SubmissionResponse;
import org.rap.algotutorbe.submission.service.SubmissionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Submission history endpoints.
 * Submission creation is handled by JudgeController (POST /judge/submit).
 */
@RestController
@RequestMapping("/submissions")
@RequiredArgsConstructor
public class SubmissionController {
    private final SubmissionService submissionService;

    @GetMapping("/{lessonSlug}")
    public ResponseEntity<ApiResponse<?>> getSubmissionOrHistory(@PathVariable String lessonSlug) {
        UUID submissionId;
        try {
            submissionId = UUID.fromString(lessonSlug);
        } catch (IllegalArgumentException ignored) {
            return ResponseEntity.ok(ApiResponse.buildSuccess(
                    submissionService.getSubmissionsByLessonSlug(lessonSlug)
            ));
        }
        return ResponseEntity.ok(ApiResponse.buildSuccess(submissionService.getSubmissionDetail(submissionId)));
    }

    @GetMapping
    public ResponseEntity<PageResponse<SubmissionResponse>> listMySubmissions(
            @RequestParam(required = false) String lessonSlug,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(submissionService.listMySubmissions(lessonSlug, status, language, page, limit));
    }
}

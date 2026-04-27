package org.rap.algotutorbe.submission.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.rap.algotutorbe.common.api.ApiResponse;
import org.rap.algotutorbe.common.api.PageResponse;
import org.rap.algotutorbe.submission.dto.SubmissionDetailResponse;
import org.rap.algotutorbe.submission.dto.SubmissionResponse;
import org.rap.algotutorbe.submission.dto.SubmitCodeRequest;
import org.rap.algotutorbe.submission.service.SubmissionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/submissions")
@RequiredArgsConstructor
public class SubmissionController {
    private final SubmissionService submissionService;

    @PostMapping
    public ResponseEntity<ApiResponse<SubmissionResponse>> submit(
            @Valid @RequestBody SubmitCodeRequest request) {
        SubmissionResponse response = submissionService.submitCode(request);
        return ResponseEntity.ok(ApiResponse.buildSuccess(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SubmissionDetailResponse>> getSubmission(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.buildSuccess(submissionService.getSubmissionDetail(id)));
    }

    @GetMapping
    public ResponseEntity<PageResponse<SubmissionResponse>> listMySubmissions(
            @RequestParam(required = false) String problemSlug,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer limit
    ) {
        return ResponseEntity.ok(submissionService.listMySubmissions(problemSlug, status, language, page, limit));
    }
}

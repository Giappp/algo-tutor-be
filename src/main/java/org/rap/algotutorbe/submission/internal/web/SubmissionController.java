package org.rap.algotutorbe.submission.internal.web;

import lombok.RequiredArgsConstructor;
import org.rap.algotutorbe.submission.internal.application.dto.SubmissionResponse;
import org.rap.algotutorbe.submission.internal.application.dto.SubmitCodeRequest;
import org.rap.algotutorbe.submission.internal.application.service.SubmissionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/submissions")
@RequiredArgsConstructor
public class SubmissionController {
    private final SubmissionService submissionService;

    @PostMapping
    public ResponseEntity<?> submit(
            @RequestBody SubmitCodeRequest request) {
        Long userId = 1L; // TODO: Get user ID from authentication context
        SubmissionResponse response = submissionService.submitCode(userId, request);
        return ResponseEntity.ok(response);
    }
}

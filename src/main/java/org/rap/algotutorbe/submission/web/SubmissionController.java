package org.rap.algotutorbe.submission.web;

import lombok.RequiredArgsConstructor;
import org.rap.algotutorbe.common.api.ApiResponse;
import org.rap.algotutorbe.submission.application.dto.SubmissionResponse;
import org.rap.algotutorbe.submission.application.dto.SubmitCodeRequest;
import org.rap.algotutorbe.submission.application.service.SubmissionService;
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

    @PostMapping("/")
    public ResponseEntity<ApiResponse<Object>> submit(
            @RequestBody SubmitCodeRequest request) {
        SubmissionResponse response = submissionService.submitCode(request);
        return ResponseEntity.ok(ApiResponse.buildSuccess(response));
    }
}

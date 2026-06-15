package org.rap.algotutorbe.judge;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.rap.algotutorbe.common.api.ApiResponse;
import org.rap.algotutorbe.common.errors.ErrorCode;
import org.rap.algotutorbe.common.exception.AppException;
import org.rap.algotutorbe.common.ratelimit.RateLimiter;
import org.rap.algotutorbe.common.services.BaseService;
import org.rap.algotutorbe.judge.dto.JudgeRequest;
import org.rap.algotutorbe.judge.dto.JudgeResponse;
import org.rap.algotutorbe.submission.dto.SubmissionDetailResponse;
import org.rap.algotutorbe.submission.service.SubmissionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/judge")
@RequiredArgsConstructor
public class JudgeController extends BaseService {

    private static final int MAX_RUNS_PER_MINUTE = 10;
    private static final int MAX_SUBMITS_PER_MINUTE = 5;
    private static final long ONE_MINUTE_MS = 60_000;

    private final JudgeService judgeService;
    private final RateLimiter rateLimiter;
    private final SubmissionService submissionService;

    /**
     * Run code against visible test cases only.
     * Does NOT save a submission record.
     * Rate limit: 10 requests/minute per user.
     */
    @PostMapping("/run")
    public ResponseEntity<ApiResponse<JudgeResponse>> run(
            @Valid @RequestBody JudgeRequest request) {
        checkRunRateLimit();
        JudgeResponse response = judgeService.run(request);
        return ResponseEntity.ok(ApiResponse.buildSuccess(response));
    }

    /**
     * Submit code against ALL test cases.
     * Saves submission record + auto-updates lesson progress.
     * Rate limit: 5 requests/minute per user.
     */
    @PostMapping("/submit")
    public ResponseEntity<ApiResponse<JudgeResponse>> submit(
            @Valid @RequestBody JudgeRequest request) {
        checkSubmitRateLimit();
        JudgeResponse response = judgeService.submit(request);
        return ResponseEntity.ok(ApiResponse.buildSuccess(response));
    }

    /**
     * Get the latest persisted state of a submission.
     * Intended as a polling fallback when WebSocket updates are unavailable.
     */
    @GetMapping("/submissions/{submissionId}")
    public ResponseEntity<ApiResponse<SubmissionDetailResponse>> getSubmission(
            @PathVariable UUID submissionId) {
        SubmissionDetailResponse response = submissionService.getSubmissionDetail(submissionId);
        return ResponseEntity.ok(ApiResponse.buildSuccess(response));
    }

    private void checkRunRateLimit() {
        String userId = getCurrentUserIdOrThrow().toString();
        if (!rateLimiter.isAllowed("judge:run:" + userId, MAX_RUNS_PER_MINUTE, ONE_MINUTE_MS)) {
            throw new AppException(ErrorCode.RATE_LIMITED);
        }
    }

    private void checkSubmitRateLimit() {
        String userId = getCurrentUserIdOrThrow().toString();
        if (!rateLimiter.isAllowed("judge:submit:" + userId, MAX_SUBMITS_PER_MINUTE, ONE_MINUTE_MS)) {
            throw new AppException(ErrorCode.RATE_LIMITED);
        }
    }
}

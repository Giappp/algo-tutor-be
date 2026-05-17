package org.rap.algotutorbe.judge;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.rap.algotutorbe.common.api.ApiResponse;
import org.rap.algotutorbe.common.api.PageResponse;
import org.rap.algotutorbe.common.errors.ErrorCode;
import org.rap.algotutorbe.common.exception.AppException;
import org.rap.algotutorbe.common.ratelimit.RateLimiter;
import org.rap.algotutorbe.common.services.BaseService;
import org.rap.algotutorbe.judge.dto.JudgeRunRequest;
import org.rap.algotutorbe.judge.dto.JudgeRunResponse;
import org.rap.algotutorbe.judge.dto.JudgeSubmitResponse;
import org.rap.algotutorbe.submission.dto.SubmissionResponse;
import org.rap.algotutorbe.submission.service.SubmissionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Judge controller for the frontend learning flow.
 * Provides synchronous code execution endpoints matching the frontend spec.
 * Rate limited: 10 runs/min, 5 submits/min per user.
 */
@RestController
@RequestMapping("/judge")
@RequiredArgsConstructor
public class JudgeController extends BaseService {

    private static final int MAX_RUNS_PER_MINUTE = 10;
    private static final int MAX_SUBMITS_PER_MINUTE = 5;
    private static final long ONE_MINUTE_MS = 60_000;

    private final JudgeRunService judgeRunService;
    private final SubmissionService submissionService;
    private final RateLimiter rateLimiter;

    /**
     * Run code against visible test cases only.
     * Does NOT save a submission record.
     * Rate limit: 10 requests/minute per user.
     */
    @PostMapping("/run")
    public ResponseEntity<ApiResponse<JudgeRunResponse>> run(
            @Valid @RequestBody JudgeRunRequest request) {
        checkRunRateLimit();
        JudgeRunResponse response = judgeRunService.run(request);
        return ResponseEntity.ok(ApiResponse.buildSuccess(response));
    }

    /**
     * Submit code against ALL test cases.
     * Saves a submission record and auto-updates lesson progress if ACCEPTED.
     * Rate limit: 5 requests/minute per user.
     */
    @PostMapping("/submit")
    public ResponseEntity<ApiResponse<JudgeSubmitResponse>> submit(
            @Valid @RequestBody JudgeRunRequest request) {
        checkSubmitRateLimit();
        JudgeSubmitResponse response = judgeRunService.submit(request);
        return ResponseEntity.ok(ApiResponse.buildSuccess(response));
    }

    /**
     * Get submission history for a lesson.
     */
    @GetMapping("/submissions")
    public ResponseEntity<PageResponse<SubmissionResponse>> getSubmissions(
            @RequestParam String lessonSlug,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ResponseEntity.ok(submissionService.listMySubmissions(
                lessonSlug, null, null, page, size));
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

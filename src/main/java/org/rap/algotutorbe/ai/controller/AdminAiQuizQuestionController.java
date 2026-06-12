package org.rap.algotutorbe.ai.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.rap.algotutorbe.ai.dto.AiQuestionSourceResponse;
import org.rap.algotutorbe.ai.dto.GenerateQuestionsFromSourcesRequest;
import org.rap.algotutorbe.ai.dto.GenerateQuestionsFromSourcesResponse;
import org.rap.algotutorbe.ai.services.AdminAiQuizQuestionService;
import org.rap.algotutorbe.common.api.ApiResponse;
import org.rap.algotutorbe.common.exception.RateLimitExceededException;
import org.rap.algotutorbe.common.ratelimit.RateLimiter;
import org.rap.algotutorbe.iam.infrastructure.SecurityUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/admin/ai/quiz-lessons")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAiQuizQuestionController {

    private static final String RATE_LIMIT_KEY_PREFIX = "admin-ai-quiz-questions:";

    private final AdminAiQuizQuestionService service;
    private final RateLimiter rateLimiter;
    private final int maxRequests;
    private final long windowMs;

    public AdminAiQuizQuestionController(
            AdminAiQuizQuestionService service,
            RateLimiter rateLimiter,
            @Value("${ai.admin-question-generation.rate-limit.max-requests:5}") int maxRequests,
            @Value("${ai.admin-question-generation.rate-limit.window-seconds:60}") int windowSeconds) {
        this.service = service;
        this.rateLimiter = rateLimiter;
        this.maxRequests = maxRequests;
        this.windowMs = windowSeconds * 1000L;
    }

    @GetMapping("/{quizLessonId}/question-sources")
    public ResponseEntity<ApiResponse<List<AiQuestionSourceResponse>>> getQuestionSources(
            @PathVariable Long quizLessonId) {
        return ResponseEntity.ok(ApiResponse.buildSuccess(service.getQuestionSources(quizLessonId)));
    }

    @PostMapping("/{quizLessonId}/generate-questions")
    public ResponseEntity<ApiResponse<GenerateQuestionsFromSourcesResponse>> generateQuestions(
            @PathVariable Long quizLessonId,
            @Valid @RequestBody GenerateQuestionsFromSourcesRequest request,
            @AuthenticationPrincipal SecurityUser principal) {
        String adminId = principal == null ? "unknown" : principal.getId().toString();
        String rateLimitKey = RATE_LIMIT_KEY_PREFIX + adminId + ":" + quizLessonId;
        if (!rateLimiter.isAllowed(rateLimitKey, maxRequests, windowMs)) {
            throw new RateLimitExceededException(
                    rateLimiter.getRetryAfterSeconds(rateLimitKey, maxRequests, windowMs));
        }

        long startedAt = System.currentTimeMillis();
        GenerateQuestionsFromSourcesResponse response = service.generateQuestions(quizLessonId, request);
        log.info(
                "Admin AI quiz generation completed adminId={} quizLessonId={} sourceLessonIds={} provider={} "
                        + "promptHash={} inputTokens={} outputTokens={} latencyMs={} result=success",
                adminId,
                quizLessonId,
                request.sourceLessonIds(),
                request.provider(),
                promptHash(request.prompt()),
                response.inputTokens(),
                response.outputTokens(),
                System.currentTimeMillis() - startedAt);
        return ResponseEntity.ok(ApiResponse.buildSuccess(response));
    }

    private String promptHash(String prompt) {
        try {
            byte[] value = (prompt == null ? "" : prompt.trim()).getBytes(StandardCharsets.UTF_8);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}

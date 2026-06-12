package org.rap.algotutorbe.ai.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.rap.algotutorbe.ai.dto.AiQuestionSourceResponse;
import org.rap.algotutorbe.ai.dto.CodingAiGenerationResponse;
import org.rap.algotutorbe.ai.dto.GenerateCodingEditorialRequest;
import org.rap.algotutorbe.ai.dto.GenerateCodingProblemRequest;
import org.rap.algotutorbe.ai.dto.GenerateStarterCodeRequest;
import org.rap.algotutorbe.ai.services.AdminAiCodingService;
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
import java.util.function.Supplier;

@Slf4j
@RestController
@RequestMapping("/admin/ai/coding-lessons")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAiCodingController {

    private static final String RATE_LIMIT_PREFIX = "admin-ai-coding:";

    private final AdminAiCodingService service;
    private final RateLimiter rateLimiter;
    private final int maxRequests;
    private final long windowMs;

    public AdminAiCodingController(
            AdminAiCodingService service,
            RateLimiter rateLimiter,
            @Value("${ai.admin-coding-generation.rate-limit.max-requests:5}") int maxRequests,
            @Value("${ai.admin-coding-generation.rate-limit.window-seconds:60}") int windowSeconds) {
        this.service = service;
        this.rateLimiter = rateLimiter;
        this.maxRequests = maxRequests;
        this.windowMs = windowSeconds * 1000L;
    }

    @GetMapping("/{lessonId}/sources")
    public ResponseEntity<ApiResponse<List<AiQuestionSourceResponse>>> getSources(@PathVariable Long lessonId) {
        return ResponseEntity.ok(ApiResponse.buildSuccess(service.getSources(lessonId)));
    }

    @PostMapping("/{lessonId}/generate-problem")
    public ResponseEntity<ApiResponse<CodingAiGenerationResponse<CodingAiGenerationResponse.ProblemContent>>>
    generateProblem(
            @PathVariable Long lessonId,
            @Valid @RequestBody GenerateCodingProblemRequest request,
            @AuthenticationPrincipal SecurityUser principal) {
        return generatedResponse(
                lessonId, principal, request.sourceLessonIds(), request.provider(), request.prompt(), "problem",
                () -> service.generateProblem(lessonId, request));
    }

    @PostMapping("/{lessonId}/generate-editorial")
    public ResponseEntity<ApiResponse<CodingAiGenerationResponse<CodingAiGenerationResponse.EditorialContent>>>
    generateEditorial(
            @PathVariable Long lessonId,
            @Valid @RequestBody GenerateCodingEditorialRequest request,
            @AuthenticationPrincipal SecurityUser principal) {
        return generatedResponse(
                lessonId, principal, request.sourceLessonIds(), request.provider(), request.prompt(), "editorial",
                () -> service.generateEditorial(lessonId, request));
    }

    @PostMapping("/{lessonId}/generate-starter-code")
    public ResponseEntity<ApiResponse<CodingAiGenerationResponse<CodingAiGenerationResponse.StarterCodeContent>>>
    generateStarterCode(
            @PathVariable Long lessonId,
            @Valid @RequestBody GenerateStarterCodeRequest request,
            @AuthenticationPrincipal SecurityUser principal) {
        return generatedResponse(
                lessonId, principal, request.sourceLessonIds(), request.provider(), request.prompt(), "starter-code",
                () -> service.generateStarterCode(lessonId, request));
    }

    private <T> ResponseEntity<ApiResponse<CodingAiGenerationResponse<T>>> generatedResponse(
            Long lessonId,
            SecurityUser principal,
            List<Long> sourceIds,
            Object provider,
            String prompt,
            String assetType,
            Supplier<CodingAiGenerationResponse<T>> generation) {
        String adminId = principal == null ? "unknown" : principal.getId().toString();
        String rateLimitKey = RATE_LIMIT_PREFIX + adminId + ":" + lessonId;
        if (!rateLimiter.isAllowed(rateLimitKey, maxRequests, windowMs)) {
            throw new RateLimitExceededException(
                    rateLimiter.getRetryAfterSeconds(rateLimitKey, maxRequests, windowMs));
        }

        long startedAt = System.currentTimeMillis();
        CodingAiGenerationResponse<T> response = generation.get();
        log.info(
                "Admin AI coding generation completed adminId={} lessonId={} sourceLessonIds={} assetType={} "
                        + "provider={} promptHash={} inputTokens={} outputTokens={} latencyMs={} result=success",
                adminId, lessonId, sourceIds, assetType, provider, promptHash(prompt),
                response.inputTokens(), response.outputTokens(), System.currentTimeMillis() - startedAt);
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

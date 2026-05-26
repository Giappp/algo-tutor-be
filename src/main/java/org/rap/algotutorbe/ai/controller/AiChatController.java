package org.rap.algotutorbe.ai.controller;

import jakarta.validation.Valid;
import org.rap.algotutorbe.ai.dto.AiChatRequest;
import org.rap.algotutorbe.ai.dto.AiChatResponse;
import org.rap.algotutorbe.ai.services.AiChatService;
import org.rap.algotutorbe.common.api.ApiResponse;
import org.rap.algotutorbe.common.exception.RateLimitExceededException;
import org.rap.algotutorbe.common.ratelimit.RateLimiter;
import org.rap.algotutorbe.iam.infrastructure.SecurityUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/ai")
public class AiChatController {

    private static final String RATE_LIMIT_KEY_PREFIX = "ai-chat:";

    private final AiChatService aiChatService;
    private final RateLimiter rateLimiter;
    private final int maxRequests;
    private final long windowMs;

    public AiChatController(
            AiChatService aiChatService,
            RateLimiter rateLimiter,
            @Value("${ai.rate-limit.max-requests:20}") int maxRequests,
            @Value("${ai.rate-limit.window-seconds:60}") int windowSeconds) {
        this.aiChatService = aiChatService;
        this.rateLimiter = rateLimiter;
        this.maxRequests = maxRequests;
        this.windowMs = windowSeconds * 1000L;
    }

    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<AiChatResponse>> chat(
            @Valid @RequestBody AiChatRequest request,
            @AuthenticationPrincipal SecurityUser principal) {
        String userId = principal.getId().toString();
        String rateLimitKey = RATE_LIMIT_KEY_PREFIX + userId;

        if (!rateLimiter.isAllowed(rateLimitKey, maxRequests, windowMs)) {
            long retryAfterSeconds = rateLimiter.getRetryAfterSeconds(rateLimitKey, maxRequests, windowMs);
            throw new RateLimitExceededException(retryAfterSeconds);
        }

        AiChatResponse response = aiChatService.chat(request, principal.getId());
        return ResponseEntity.ok(ApiResponse.buildSuccess(response));
    }

    @PostMapping(value = "/chat/stream", produces = org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(
            @Valid @RequestBody AiChatRequest request,
            @AuthenticationPrincipal SecurityUser principal) {
        String userId = principal.getId().toString();
        String rateLimitKey = RATE_LIMIT_KEY_PREFIX + userId;

        if (!rateLimiter.isAllowed(rateLimitKey, maxRequests, windowMs)) {
            long retryAfterSeconds = rateLimiter.getRetryAfterSeconds(rateLimitKey, maxRequests, windowMs);
            throw new RateLimitExceededException(retryAfterSeconds);
        }

        SseEmitter emitter = new SseEmitter(60000L);
        aiChatService.chatStream(request, principal.getId(), emitter);
        return emitter;
    }

    @GetMapping("/chat/bootstrap")
    public ResponseEntity<ApiResponse<AiChatResponse>> bootstrapChat(@RequestParam(name = "lessonSlug") String lessonSlug,
                                                                     @AuthenticationPrincipal SecurityUser principal) {
        var response = aiChatService.bootstrap(principal.getId(), lessonSlug);
        return ResponseEntity.ok(ApiResponse.buildSuccess(response));
    }
}

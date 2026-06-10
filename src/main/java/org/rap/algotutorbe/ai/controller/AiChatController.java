package org.rap.algotutorbe.ai.controller;

import jakarta.validation.Valid;
import org.rap.algotutorbe.ai.dto.AiChatHistoryResponse;
import org.rap.algotutorbe.ai.dto.AiChatResponse;
import org.rap.algotutorbe.ai.dto.AiGeneralChatRequest;
import org.rap.algotutorbe.ai.dto.AiGeneralChatResponse;
import org.rap.algotutorbe.ai.dto.AiLessonChatRequest;
import org.rap.algotutorbe.ai.dto.AiRoadmapAdvisoryResponse.RoadmapInfo;
import org.rap.algotutorbe.ai.services.AiChatHistoryService;
import org.rap.algotutorbe.ai.services.AiGeneralChatService;
import org.rap.algotutorbe.ai.services.AiLessonChatService;
import org.rap.algotutorbe.common.api.ApiResponse;
import org.rap.algotutorbe.common.exception.RateLimitExceededException;
import org.rap.algotutorbe.common.ratelimit.RateLimiter;
import org.rap.algotutorbe.iam.infrastructure.SecurityUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/ai")
public class AiChatController {

    private static final String RATE_LIMIT_KEY_PREFIX = "ai-chat:";

    private final AiGeneralChatService aiGeneralChatService;
    private final AiLessonChatService aiLessonChatService;
    private final AiChatHistoryService aiChatHistoryService;
    private final RateLimiter rateLimiter;
    private final int maxRequests;
    private final long windowMs;

    public AiChatController(
            AiGeneralChatService aiGeneralChatService,
            AiLessonChatService aiLessonChatService,
            AiChatHistoryService aiChatHistoryService,
            RateLimiter rateLimiter,
            @Value("${ai.rate-limit.max-requests:20}") int maxRequests,
            @Value("${ai.rate-limit.window-seconds:60}") int windowSeconds) {
        this.aiGeneralChatService = aiGeneralChatService;
        this.aiLessonChatService = aiLessonChatService;
        this.aiChatHistoryService = aiChatHistoryService;
        this.rateLimiter = rateLimiter;
        this.maxRequests = maxRequests;
        this.windowMs = windowSeconds * 1000L;
    }

    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<AiChatResponse>> chat(
            @Valid @RequestBody AiLessonChatRequest request,
            @AuthenticationPrincipal SecurityUser principal) {
        String userId = principal.getId().toString();
        String rateLimitKey = RATE_LIMIT_KEY_PREFIX + userId;

        if (!rateLimiter.isAllowed(rateLimitKey, maxRequests, windowMs)) {
            long retryAfterSeconds = rateLimiter.getRetryAfterSeconds(rateLimitKey, maxRequests, windowMs);
            throw new RateLimitExceededException(retryAfterSeconds);
        }

        AiChatResponse response = aiLessonChatService.chat(request, principal.getId());
        return ResponseEntity.ok(ApiResponse.buildSuccess(response));
    }

    @PostMapping(value = "/chat/stream", produces = org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(
            @Valid @RequestBody AiLessonChatRequest request,
            @AuthenticationPrincipal SecurityUser principal) {
        String userId = principal.getId().toString();
        String rateLimitKey = RATE_LIMIT_KEY_PREFIX + userId;

        if (!rateLimiter.isAllowed(rateLimitKey, maxRequests, windowMs)) {
            long retryAfterSeconds = rateLimiter.getRetryAfterSeconds(rateLimitKey, maxRequests, windowMs);
            throw new RateLimitExceededException(retryAfterSeconds);
        }

        SseEmitter emitter = new SseEmitter(60000L);
        aiLessonChatService.chatStream(request, principal.getId(), emitter);
        return emitter;
    }

    @PostMapping(value = "/general/chat/stream", produces = org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter generalChatStream(
            @Valid @RequestBody AiGeneralChatRequest request,
            @AuthenticationPrincipal SecurityUser principal) {
        String rateLimitKey = "ai-general-chat:" + principal.getId().toString();

        if (!rateLimiter.isAllowed(rateLimitKey, maxRequests, windowMs)) {
            long retryAfterSeconds = rateLimiter.getRetryAfterSeconds(rateLimitKey, maxRequests, windowMs);
            throw new RateLimitExceededException(retryAfterSeconds);
        }

        // Fetch published roadmaps inside a short read-only transaction first
        List<RoadmapInfo> availableRoadmaps = aiGeneralChatService.getRoadmapsForAdvisory();

        SseEmitter emitter = new SseEmitter(60000L);
        aiGeneralChatService.generalChatStream(request, principal.getId(), emitter, availableRoadmaps);
        return emitter;
    }

    @PostMapping("/general/chat")
    public ResponseEntity<ApiResponse<AiGeneralChatResponse>> generalChat(
            @Valid @RequestBody AiGeneralChatRequest request,
            @AuthenticationPrincipal SecurityUser principal) {
        String rateLimitKey = "ai-general-chat:" + principal.getId().toString();

        if (!rateLimiter.isAllowed(rateLimitKey, maxRequests, windowMs)) {
            long retryAfterSeconds = rateLimiter.getRetryAfterSeconds(rateLimitKey, maxRequests, windowMs);
            throw new RateLimitExceededException(retryAfterSeconds);
        }

        List<RoadmapInfo> availableRoadmaps = aiGeneralChatService.getRoadmapsForAdvisory();
        AiGeneralChatResponse response = aiGeneralChatService.generalChat(request, principal.getId(), availableRoadmaps);
        return ResponseEntity.ok(ApiResponse.buildSuccess(response));
    }

    @GetMapping("/chat/bootstrap")
    public ResponseEntity<ApiResponse<AiChatResponse>> bootstrapChat(@RequestParam(name = "lessonSlug") String lessonSlug,
                                                                     @AuthenticationPrincipal SecurityUser principal) {
        var response = aiLessonChatService.bootstrap(principal.getId(), lessonSlug);
        return ResponseEntity.ok(ApiResponse.buildSuccess(response));
    }

    @GetMapping("/chat/history/{conversationId}")
    public ResponseEntity<ApiResponse<AiChatHistoryResponse>> getLessonChatHistory(
            @PathVariable UUID conversationId,
            @AuthenticationPrincipal SecurityUser principal) {
        var response = aiChatHistoryService.getLessonChatHistory(conversationId, principal.getId());
        return ResponseEntity.ok(ApiResponse.buildSuccess(response));
    }

    @GetMapping("/general/chat/history/{conversationId}")
    public ResponseEntity<ApiResponse<AiChatHistoryResponse>> getGeneralChatHistory(
            @PathVariable UUID conversationId,
            @AuthenticationPrincipal SecurityUser principal) {
        var response = aiChatHistoryService.getGeneralChatHistory(conversationId, principal.getId());
        return ResponseEntity.ok(ApiResponse.buildSuccess(response));
    }
}

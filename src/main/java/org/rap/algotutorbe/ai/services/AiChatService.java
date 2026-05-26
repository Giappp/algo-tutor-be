package org.rap.algotutorbe.ai.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rap.algotutorbe.ai.dto.AiChatRequest;
import org.rap.algotutorbe.ai.dto.AiChatResponse;
import org.rap.algotutorbe.ai.dto.AiChunkResponse;
import org.rap.algotutorbe.ai.entity.AIConversation;
import org.rap.algotutorbe.ai.entity.AiMessage;
import org.rap.algotutorbe.ai.enums.AiChatMode;
import org.rap.algotutorbe.ai.enums.AiMessageRole;
import org.rap.algotutorbe.ai.enums.LLMProvider;
import org.rap.algotutorbe.ai.repository.AiMessageRepository;
import org.rap.algotutorbe.ai.repository.ConversationRepository;
import org.rap.algotutorbe.ai.tools.AlgoTutorAiTools;
import org.rap.algotutorbe.common.errors.ErrorCode;
import org.rap.algotutorbe.common.exception.AppException;
import org.rap.algotutorbe.learning.models.Lesson;
import org.rap.algotutorbe.learning.repositories.LessonRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatService {

    private static final int MAX_MESSAGE_LENGTH = 5000;
    private static final int MAX_CODE_LENGTH = 10000;
    private static final int MAX_TITLE_LENGTH = 255;
    private static final Set<AiChatMode> CODE_REQUIRED_MODES = Set.of(
            AiChatMode.DEBUG, AiChatMode.REVIEW, AiChatMode.COMPLEXITY);

    private final AiContextService aiContextService;
    private final AiPromptService aiPromptService;
    private final ConversationRepository conversationRepository;
    private final ProviderRouter providerRouter;
    private final LessonRepository lessonRepository;
    private final AiMessageRepository aiMessageRepository;
    private final AlgoTutorAiTools algoTutorAiTools;
    private final SuggestionGenerator suggestionGenerator;
    private final AiMessagePersister aiMessagePersister;

    public AiChatResponse chat(AiChatRequest request, UUID userId) {
        validateRequest(request);

        // Resolve provider
        LLMProvider provider = resolveProvider(request.provider());

        // Find or create conversation
        AIConversation conversation = findOrCreateConversation(request, userId, provider);

        // Update updatedAt timestamp (triggers @PreUpdate)
        conversationRepository.save(conversation);

        // Build context from lesson, judge result, error, code
        String context = aiContextService.buildContext(request);

        AiChatMode mode = AiChatMode.valueOf(request.mode().toUpperCase());

        // Determine lesson type and hints count
        org.rap.algotutorbe.learning.enums.LessonType lessonType = org.rap.algotutorbe.learning.enums.LessonType.THEORY;
        boolean canAskNextHintBefore = false;
        int maxAllowedHints = 0;
        long hintMessagesCount = 0;
        boolean isCodingLesson = false;

        if (conversation.getLessonId() != null) {
            var lessonOpt = lessonRepository.findById(conversation.getLessonId());
            if (lessonOpt.isPresent()) {
                Lesson lesson = lessonOpt.get();
                lessonType = lesson.getType();
                if (lesson instanceof org.rap.algotutorbe.learning.models.CodingLesson codingLesson) {
                    isCodingLesson = true;
                    hintMessagesCount = aiMessageRepository.countByConversationIdAndRoleAndMode(
                            conversation.getId(), AiMessageRole.ASSISTANT, "HINT");
                    maxAllowedHints = Math.min(5, codingLesson.getHints().size());
                    canAskNextHintBefore = hintMessagesCount < maxAllowedHints;
                }
            }
        }

        if (mode == AiChatMode.HINT && isCodingLesson && !canAskNextHintBefore) {
            throw new AppException(ErrorCode.NO_MORE_HINTS);
        }

        String systemPrompt = aiPromptService.buildSystemPrompt(mode);
        String userPrompt = aiPromptService.buildUserPrompt(request, context, "");

        // Assemble native messages
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemPrompt));
        messages.addAll(getHistoryAsNativeMessages(conversation.getId()));
        messages.add(new UserMessage(userPrompt));

        // Route to LLM provider and call with tools, handle 30s timeout and token usage
        ChatResponseWithTokens responseWithTokens = callLlmWithTokens(request.provider(), messages);
        String aiResponse = responseWithTokens.responseText();

        // Persist user message
        String userContent = (request.message() != null && !request.message().isBlank())
                ? request.message()
                : request.code();
        aiMessagePersister.saveMessage(conversation.getId(), userId, AiMessageRole.USER, userContent, request.mode().toUpperCase(),
                responseWithTokens.inputTokens(), null);

        // Persist assistant message
        aiMessagePersister.saveMessage(conversation.getId(), userId, AiMessageRole.ASSISTANT, aiResponse,
                request.mode().toUpperCase(),
                null, responseWithTokens.outputTokens());

        boolean canAskNextHintAfter = false;
        if (isCodingLesson) {
            long finalHintCount = mode == AiChatMode.HINT ? hintMessagesCount + 1 : hintMessagesCount;
            canAskNextHintAfter = finalHintCount < maxAllowedHints;
        }

        // Generate quick actions
        boolean hasCode = request.code() != null && !request.code().isBlank();
        boolean hasError = request.errorMessage() != null && !request.errorMessage().isBlank();
        var quickActions = suggestionGenerator.generate(
                mode,
                lessonType,
                hasCode,
                hasError,
                canAskNextHintAfter);

        // Assemble and return response
        return new AiChatResponse(
                conversation.getId(),
                aiResponse,
                request.mode(),
                quickActions,
                Collections.emptyList(),
                isCodingLesson ? canAskNextHintAfter : null);
    }

    public void chatStream(AiChatRequest request, UUID userId, SseEmitter emitter) {
        validateRequest(request);

        // Resolve provider
        LLMProvider provider = resolveProvider(request.provider());

        // Find or create conversation
        AIConversation conversation = findOrCreateConversation(request, userId, provider);

        // Update updatedAt timestamp
        conversationRepository.save(conversation);

        // Build context
        String context = aiContextService.buildContext(request);

        AiChatMode mode = AiChatMode.valueOf(request.mode().toUpperCase());

        // Determine lesson type and hints count
        org.rap.algotutorbe.learning.enums.LessonType lessonType = org.rap.algotutorbe.learning.enums.LessonType.THEORY;
        boolean canAskNextHintBefore = false;
        int maxAllowedHints = 0;
        long hintMessagesCount = 0;
        boolean isCodingLesson = false;

        if (conversation.getLessonId() != null) {
            var lessonOpt = lessonRepository.findById(conversation.getLessonId());
            if (lessonOpt.isPresent()) {
                Lesson lesson = lessonOpt.get();
                lessonType = lesson.getType();
                if (lesson instanceof org.rap.algotutorbe.learning.models.CodingLesson codingLesson) {
                    isCodingLesson = true;
                    hintMessagesCount = aiMessageRepository.countByConversationIdAndRoleAndMode(
                            conversation.getId(), AiMessageRole.ASSISTANT, "HINT");
                    maxAllowedHints = Math.min(5, codingLesson.getHints().size());
                    canAskNextHintBefore = hintMessagesCount < maxAllowedHints;
                }
            }
        }

        if (mode == AiChatMode.HINT && isCodingLesson && !canAskNextHintBefore) {
            emitter.completeWithError(new AppException(ErrorCode.NO_MORE_HINTS));
            return;
        }

        String systemPrompt = aiPromptService.buildSystemPrompt(mode);
        String userPrompt = aiPromptService.buildUserPrompt(request, context, "");

        // Assemble native messages
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemPrompt));
        messages.addAll(getHistoryAsNativeMessages(conversation.getId()));
        messages.add(new UserMessage(userPrompt));

        try {
            ChatClient chatClient = providerRouter.route(request.provider());
            Flux<ChatResponse> stream = chatClient.prompt()
                    .messages(messages)
                    .tools(algoTutorAiTools)
                    .stream()
                    .chatResponse();

            StringBuilder fullAnswer = new StringBuilder();
            java.util.concurrent.atomic.AtomicInteger inputTokensRef = new java.util.concurrent.atomic.AtomicInteger(0);
            java.util.concurrent.atomic.AtomicInteger outputTokensRef = new java.util.concurrent.atomic.AtomicInteger(
                    0);

            boolean finalIsCodingLesson = isCodingLesson;
            long finalHintMessagesCount = hintMessagesCount;
            int finalMaxAllowedHints = maxAllowedHints;
            org.rap.algotutorbe.learning.enums.LessonType finalLessonType = lessonType;

            reactor.core.Disposable subscription = stream.subscribe(
                    chatResponse -> {
                        if (chatResponse != null) {
                            // Extract usage metadata if available in this chunk
                            if (chatResponse.getMetadata() != null && chatResponse.getMetadata().getUsage() != null) {
                                org.springframework.ai.chat.metadata.Usage usage = chatResponse.getMetadata()
                                        .getUsage();
                                if (usage.getPromptTokens() != null) {
                                    inputTokensRef.set(usage.getPromptTokens().intValue());
                                }
                                if (usage.getCompletionTokens() != null) {
                                    outputTokensRef.set(usage.getCompletionTokens().intValue());
                                }
                            }

                            if (chatResponse.getResult() != null && chatResponse.getResult().getOutput() != null) {
                                String chunkText = chatResponse.getResult().getOutput().getText();
                                if (chunkText != null) {
                                    fullAnswer.append(chunkText);
                                    try {
                                        emitter.send(SseEmitter.event()
                                                .name("message")
                                                .data(new AiChunkResponse(chunkText)));
                                    } catch (Exception e) {
                                        log.error("Failed to send SSE chunk", e);
                                    }
                                }
                            }
                        }
                    },
                    error -> {
                        log.error("LLM stream error for provider [{}]", request.provider(), error);
                        emitter.completeWithError(new AppException(ErrorCode.AI_SERVICE_UNAVAILABLE, error));
                    },
                    () -> {
                        try {
                            // Persist user message
                            String userContent = (request.message() != null && !request.message().isBlank())
                                    ? request.message()
                                    : request.code();

                            Integer finalInputTokens = inputTokensRef.get() > 0 ? inputTokensRef.get() : null;
                            Integer finalOutputTokens = outputTokensRef.get() > 0 ? outputTokensRef.get() : null;

                            aiMessagePersister.saveMessage(conversation.getId(), userId, AiMessageRole.USER,
                                    userContent, request.mode().toUpperCase(),
                                    finalInputTokens, null);

                            // Persist assistant message
                            aiMessagePersister.saveMessage(conversation.getId(), userId, AiMessageRole.ASSISTANT,
                                    fullAnswer.toString(),
                                    request.mode().toUpperCase(), null, finalOutputTokens);

                            boolean canAskNextHintAfter = false;
                            if (finalIsCodingLesson) {
                                long finalHintCount = mode == AiChatMode.HINT ? finalHintMessagesCount + 1
                                        : finalHintMessagesCount;
                                canAskNextHintAfter = finalHintCount < finalMaxAllowedHints;
                            }

                            // Generate quick actions
                            boolean hasCode = request.code() != null && !request.code().isBlank();
                            boolean hasError = request.errorMessage() != null && !request.errorMessage().isBlank();
                            var quickActions = suggestionGenerator.generate(
                                    mode,
                                    finalLessonType,
                                    hasCode,
                                    hasError,
                                    canAskNextHintAfter);

                            AiChatResponse metadata = new AiChatResponse(
                                    conversation.getId(),
                                    null,
                                    request.mode(),
                                    quickActions,
                                    Collections.emptyList(),
                                    finalIsCodingLesson ? canAskNextHintAfter : null);

                            emitter.send(SseEmitter.event()
                                    .name("metadata")
                                    .data(metadata));
                            emitter.complete();
                        } catch (Exception e) {
                            log.error("Error in stream completion callback", e);
                            emitter.completeWithError(e);
                        }
                    });

            emitter.onCompletion(subscription::dispose);
            emitter.onTimeout(subscription::dispose);
            emitter.onError(e -> subscription.dispose());
        } catch (Exception e) {
            log.error("Failed to initiate LLM stream", e);
            emitter.completeWithError(new AppException(ErrorCode.AI_SERVICE_UNAVAILABLE, e));
        }
    }

    /**
     * Finds an existing conversation by ID with ownership validation,
     * or creates a new conversation when conversationId is null.
     */
    AIConversation findOrCreateConversation(AiChatRequest request, UUID userId, LLMProvider provider) {
        if (request.conversationId() != null) {
            return conversationRepository.findByIdAndUserId(request.conversationId(), userId)
                    .orElseThrow(() -> new AppException(ErrorCode.CONVERSATION_NOT_FOUND));
        }

        // Create new conversation
        AIConversation conversation = new AIConversation();
        conversation.setUserId(userId);
        conversation.setLessonId(request.lessonId());
        conversation.setProvider(provider);
        conversation.setTitle(generateTitle(request));

        return conversationRepository.save(conversation);
    }

    /**
     * Generates a conversation title from the first user message (truncated to 255
     * chars)
     * or falls back to the lesson title if available.
     */
    private String generateTitle(AiChatRequest request) {
        // Prefer first user message as title
        if (request.message() != null && !request.message().isBlank()) {
            String message = request.message().trim();
            if (message.length() > MAX_TITLE_LENGTH) {
                return message.substring(0, MAX_TITLE_LENGTH);
            }
            return message;
        }

        // Fall back to lesson title
        if (request.lessonId() != null) {
            return lessonRepository.findById(request.lessonId())
                    .map(lesson -> {
                        String title = lesson.getTitle();
                        if (title != null && title.length() > MAX_TITLE_LENGTH) {
                            return title.substring(0, MAX_TITLE_LENGTH);
                        }
                        return title;
                    })
                    .orElse("New Conversation");
        }

        return "New Conversation";
    }

    /**
     * Calls the LLM provider with the assembled native messages and registered
     * tools.
     * Handles timeout and provider errors by catching exceptions and throwing
     * AI_SERVICE_UNAVAILABLE.
     * Captures and returns token usage from the ChatResponse.
     */
    ChatResponseWithTokens callLlmWithTokens(String providerName,
            List<org.springframework.ai.chat.messages.Message> messages) {
        try {
            ChatClient chatClient = providerRouter.route(providerName);
            org.springframework.ai.chat.model.ChatResponse chatResponse = chatClient.prompt()
                    .messages(messages)
                    .tools(algoTutorAiTools)
                    .call()
                    .chatResponse();

            if (chatResponse == null || chatResponse.getResult() == null
                    || chatResponse.getResult().getOutput() == null) {
                throw new AppException(ErrorCode.AI_SERVICE_UNAVAILABLE);
            }

            String responseText = chatResponse.getResult().getOutput().getText();
            if (responseText == null || responseText.isBlank()) {
                throw new AppException(ErrorCode.AI_SERVICE_UNAVAILABLE);
            }

            Integer inputTokens = null;
            Integer outputTokens = null;
            if (chatResponse.getMetadata() != null && chatResponse.getMetadata().getUsage() != null) {
                org.springframework.ai.chat.metadata.Usage usage = chatResponse.getMetadata().getUsage();
                if (usage.getPromptTokens() != null) {
                    inputTokens = usage.getPromptTokens().intValue();
                }
                if (usage.getCompletionTokens() != null) {
                    outputTokens = usage.getCompletionTokens().intValue();
                }
            }

            return new ChatResponseWithTokens(responseText, inputTokens, outputTokens);
        } catch (AppException e) {
            // Re-throw AppExceptions (like UNSUPPORTED_PROVIDER) as-is
            throw e;
        } catch (Exception e) {
            if (isTimeoutException(e)) {
                log.error("LLM call timed out for provider [{}]", providerName, e);
            } else {
                log.error("LLM provider error for provider [{}]", providerName, e);
            }
            throw new AppException(ErrorCode.AI_SERVICE_UNAVAILABLE, e);
        }
    }

    /**
     * Determines if an exception is a timeout-related exception.
     * Checks the exception chain for common timeout indicators.
     */
    private boolean isTimeoutException(Exception e) {
        Throwable current = e;
        while (current != null) {
            if (current instanceof java.net.SocketTimeoutException
                    || current instanceof java.util.concurrent.TimeoutException) {
                return true;
            }
            String message = current.getMessage();
            if (message != null && (message.toLowerCase().contains("timed out")
                    || message.toLowerCase().contains("timeout")
                    || message.toLowerCase().contains("read timed out"))) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private LLMProvider resolveProvider(String providerName) {
        if (providerName == null || providerName.isBlank()) {
            return LLMProvider.OPENAI; // default provider
        }
        try {
            return LLMProvider.valueOf(providerName.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.UNSUPPORTED_PROVIDER);
        }
    }

    void validateRequest(AiChatRequest request) {
        // 1. Validate mode is a valid AiChatMode value
        AiChatMode mode = parseMode(request.mode());

        // 2. Validate message length ≤ 5000
        if (request.message() != null && request.message().length() > MAX_MESSAGE_LENGTH) {
            throw new AppException(ErrorCode.INVALID_PAYLOAD);
        }

        // 3. Validate code length ≤ 10000
        if (request.code() != null && request.code().length() > MAX_CODE_LENGTH) {
            throw new AppException(ErrorCode.INVALID_PAYLOAD);
        }

        // 4. Validate at least one of message/code is non-blank
        boolean messageBlank = request.message() == null || request.message().isBlank();
        boolean codeBlank = request.code() == null || request.code().isBlank();
        if (messageBlank && codeBlank) {
            throw new AppException(ErrorCode.INVALID_PAYLOAD);
        }

        // 5. Validate code-required modes (DEBUG, REVIEW, COMPLEXITY) have non-blank
        // code
        if (CODE_REQUIRED_MODES.contains(mode) && codeBlank) {
            throw new AppException(ErrorCode.CODE_REQUIRED);
        }
    }

    private AiChatMode parseMode(String mode) {
        try {
            return AiChatMode.valueOf(mode.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new AppException(ErrorCode.INVALID_CHAT_MODE);
        }
    }

    public AiChatResponse bootstrap(UUID userId, String lessonSlug) {
        var lesson = resolveLessonBySlug(lessonSlug);

        AIConversation conversation = findOrCreateBootstrapConversation(userId, lesson);

        String greeting = buildOnboardingMessage(lesson);

        return new AiChatResponse(
                conversation.getId(),
                greeting,
                "BOOTSTRAP",
                null,
                Collections.emptyList(),
                true);
    }

    private Lesson resolveLessonBySlug(String lessonSlug) {
        return lessonRepository.findBySlug(lessonSlug)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));
    }

    private AIConversation findOrCreateBootstrapConversation(UUID userId, Lesson lesson) {
        if (lesson != null && lesson.getId() != null) {
            return conversationRepository
                    .findTopByUserIdAndLessonIdOrderByUpdatedAtDesc(userId, lesson.getId())
                    .orElseGet(() -> createBootstrapConversation(userId, lesson));
        }

        AIConversation conversation = new AIConversation();
        conversation.setUserId(userId);
        conversation.setTitle("New AI Assistant Chat");

        return conversationRepository.save(conversation);
    }

    private AIConversation createBootstrapConversation(UUID userId, Lesson lesson) {
        AIConversation conversation = new AIConversation();
        conversation.setUserId(userId);
        conversation.setLessonId(lesson.getId());
        conversation.setProvider(LLMProvider.OPENAI);
        conversation.setTitle("Chat về " + safeTitle(lesson.getTitle()));

        return conversationRepository.save(conversation);
    }

    private String buildOnboardingMessage(Lesson lesson) {
        if (lesson == null) {
            return """
                    Xin chào, mình là AlgoTutor AI.

                    Mình có thể giúp bạn:
                    - Giải thích kiến thức thuật toán
                    - Gợi ý hướng giải từng bước
                    - Kiểm tra và debug code
                    - Phân tích độ phức tạp

                    Bạn muốn bắt đầu với phần nào?
                    """;
        }

        return """
                Bạn đang học bài "%s".

                Mình có thể hỗ trợ bạn theo từng bước:
                - Giải thích lại nội dung bài học
                - Đưa gợi ý nhẹ trước, không tiết lộ lời giải ngay
                - Kiểm tra code nếu bạn gửi lên
                - Phân tích độ phức tạp và edge cases

                Bạn muốn mình hỗ trợ theo hướng nào?
                """.formatted(safeTitle(lesson.getTitle()));
    }

    private String safeTitle(String title) {
        if (title == null || title.isBlank()) {
            return "bài học này";
        }

        if (title.length() > MAX_TITLE_LENGTH) {
            return title.substring(0, MAX_TITLE_LENGTH);
        }

        return title;
    }

    private record ChatResponseWithTokens(String responseText, Integer inputTokens, Integer outputTokens) {
    }

    private List<Message> getHistoryAsNativeMessages(UUID conversationId) {
        List<AiMessage> dbMessages = aiMessageRepository.findTop10ByConversationIdOrderByCreatedAtDesc(conversationId);
        if (dbMessages.isEmpty()) {
            return Collections.emptyList();
        }
        Collections.reverse(dbMessages);
        List<Message> nativeMessages = new ArrayList<>();
        for (AiMessage dbMsg : dbMessages) {
            if (dbMsg.getRole() == AiMessageRole.USER) {
                nativeMessages.add(new UserMessage(dbMsg.getContent()));
            } else if (dbMsg.getRole() == AiMessageRole.ASSISTANT) {
                nativeMessages.add(new AssistantMessage(dbMsg.getContent()));
            }
        }
        return nativeMessages;
    }
}
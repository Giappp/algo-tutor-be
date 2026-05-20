package org.rap.algotutorbe.ai.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rap.algotutorbe.ai.dto.AiChatRequest;
import org.rap.algotutorbe.ai.dto.AiChatResponse;
import org.rap.algotutorbe.ai.dto.AiSuggestion;
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
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatService {

    private static final int MAX_MESSAGE_LENGTH = 5000;
    private static final int MAX_CODE_LENGTH = 10000;
    private static final int MAX_TITLE_LENGTH = 255;
    private static final int MAX_HISTORY_MESSAGES = 10;
    private static final int MIN_HISTORY_MESSAGES = 4;
    private static final int MAX_CONTEXT_WINDOW_CHARS = 100_000;
    private static final Set<AiChatMode> CODE_REQUIRED_MODES = Set.of(
            AiChatMode.DEBUG, AiChatMode.REVIEW, AiChatMode.COMPLEXITY
    );

    private final AiContextService aiContextService;
    private final AiPromptService aiPromptService;
    private final ConversationRepository conversationRepository;
    private final ProviderRouter providerRouter;
    private final LessonRepository lessonRepository;
    private final AiMessageRepository aiMessageRepository;
    private final AlgoTutorAiTools algoTutorAiTools;
    private final SuggestionGenerator suggestionGenerator;

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

        // Retrieve conversation history (up to 10 messages, oldest-to-newest)
        String history = buildConversationHistory(conversation.getId());

        // Build system prompt (mode-specific) and user prompt
        AiChatMode mode = AiChatMode.valueOf(request.mode().toUpperCase());
        String systemPrompt = aiPromptService.buildSystemPrompt(mode);
        String userPrompt = aiPromptService.buildUserPrompt(request, context, history);

        // Route to LLM provider and call with tools, handle 30s timeout
        String aiResponse = callLlm(request.provider(), systemPrompt, userPrompt);

        // Persist user message
        String userContent = (request.message() != null && !request.message().isBlank())
                ? request.message()
                : request.code();
        saveMessage(conversation.getId(), userId, AiMessageRole.USER, userContent, request.mode(), null, null);

        // Persist assistant message
        saveMessage(conversation.getId(), userId, AiMessageRole.ASSISTANT, aiResponse, request.mode(), null, null);

        // Assemble and return response
        return new AiChatResponse(
                conversation.getId(),
                aiResponse,
                request.mode(),
                Collections.emptyList(),
                Collections.emptyList(),
                null
        );
    }

    /**
     * Saves a message to the database with the given parameters.
     * Token counts are set to null for now (Spring AI's .content() doesn't expose usage info).
     */
    private AiMessage saveMessage(UUID conversationId, UUID userId, AiMessageRole role,
                                  String content, String mode, Integer tokenInput, Integer tokenOutput) {
        AiMessage message = new AiMessage();
        message.setConversationId(conversationId);
        message.setUserId(userId);
        message.setRole(role);
        message.setContent(content);
        message.setMode(mode);
        message.setTokenInput(tokenInput);
        message.setTokenOutput(tokenOutput);
        return aiMessageRepository.save(message);
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
     * Generates a conversation title from the first user message (truncated to 255 chars)
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
     * Retrieves up to 10 most recent messages for a conversation, ordered oldest-to-newest.
     * If the total history string exceeds the context window limit, truncates from the oldest
     * while preserving at least 4 most recent messages (2 user-assistant exchanges).
     */
    String buildConversationHistory(UUID conversationId) {
        List<AiMessage> messages = aiMessageRepository
                .findTop10ByConversationIdOrderByCreatedAtDesc(conversationId);

        if (messages.isEmpty()) {
            return "";
        }

        // Reverse to get oldest-to-newest order
        Collections.reverse(messages);

        // Format all messages as "Role: content"
        String fullHistory = formatHistory(messages);

        // If history exceeds context window, truncate from oldest, keep at least 4 most recent
        if (fullHistory.length() > MAX_CONTEXT_WINDOW_CHARS && messages.size() > MIN_HISTORY_MESSAGES) {
            List<AiMessage> truncated = messages.subList(
                    Math.max(0, messages.size() - MIN_HISTORY_MESSAGES),
                    messages.size()
            );
            return formatHistory(truncated);
        }

        return fullHistory;
    }

    private String formatHistory(List<AiMessage> messages) {
        return messages.stream()
                .map(msg -> msg.getRole().name() + ": " + msg.getContent())
                .collect(Collectors.joining("\n"));
    }

    /**
     * Calls the LLM provider with the assembled prompts and registered tools.
     * Handles timeout and provider errors by catching exceptions and throwing AI_SERVICE_UNAVAILABLE.
     * No internal details (API keys, connection strings, class names) are exposed to the client.
     */
    String callLlm(String providerName, String systemPrompt, String userPrompt) {
        try {
            ChatClient chatClient = providerRouter.route(providerName);
            String response = chatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .tools(algoTutorAiTools)
                    .call()
                    .content();

            if (response == null || response.isBlank()) {
                throw new AppException(ErrorCode.AI_SERVICE_UNAVAILABLE);
            }

            return response;
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

        // 5. Validate code-required modes (DEBUG, REVIEW, COMPLEXITY) have non-blank code
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

        List<AiSuggestion> suggestions = suggestionGenerator.buildSuggestionsForMode(AiChatMode.EXPLAIN, lesson.getType().toString());

        return new AiChatResponse(
                conversation.getId(),
                greeting,
                "BOOTSTRAP",
                suggestions,
                Collections.emptyList(),
                true
        );
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
}
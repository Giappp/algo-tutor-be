package org.rap.algotutorbe.ai.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rap.algotutorbe.ai.dto.AiChatRequest;
import org.rap.algotutorbe.ai.dto.AiChatResponse;
import org.rap.algotutorbe.ai.dto.AiLessonChatRequest;
import org.rap.algotutorbe.ai.dto.AiQuickAction;
import org.rap.algotutorbe.ai.entity.AIConversation;
import org.rap.algotutorbe.ai.entity.AiMessage;
import org.rap.algotutorbe.ai.enums.AiChatMode;
import org.rap.algotutorbe.ai.enums.AiMessageRole;
import org.rap.algotutorbe.ai.enums.ConversationType;
import org.rap.algotutorbe.ai.enums.LLMProvider;
import org.rap.algotutorbe.ai.repository.AiMessageRepository;
import org.rap.algotutorbe.ai.repository.ConversationRepository;
import org.rap.algotutorbe.ai.tools.AlgoTutorAiTools;
import org.rap.algotutorbe.common.errors.ErrorCode;
import org.rap.algotutorbe.common.exception.AppException;
import org.rap.algotutorbe.learning.enums.LessonType;
import org.rap.algotutorbe.learning.enums.ProgressStatus;
import org.rap.algotutorbe.learning.models.CodingLesson;
import org.rap.algotutorbe.learning.models.Lesson;
import org.rap.algotutorbe.learning.repositories.LessonProgressRepository;
import org.rap.algotutorbe.learning.repositories.LessonRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiLessonChatService {

    private static final int MAX_MESSAGE_LENGTH = 5_000;
    private static final int MAX_CODE_LENGTH = 10_000;
    private static final int MAX_TITLE_LENGTH = 255;
    private static final int MAX_CODING_HINTS = 5;

    private static final String DEFAULT_CONVERSATION_TITLE = "New Conversation";
    private static final String DEFAULT_AI_ASSISTANT_TITLE = "New AI Assistant Chat";
    private static final String BOOTSTRAP_MODE = "BOOTSTRAP";
    private static final String HINT_MODE = AiChatMode.HINT.name();

    private static final Set<AiChatMode> CODE_REQUIRED_MODES = Set.of(
            AiChatMode.DEBUG,
            AiChatMode.REVIEW,
            AiChatMode.COMPLEXITY);

    private final AiContextService aiContextService;
    private final AiPromptService aiPromptService;
    private final ConversationRepository conversationRepository;
    private final ProviderRouter providerRouter;
    private final LessonRepository lessonRepository;
    private final AiMessageRepository aiMessageRepository;
    private final AlgoTutorAiTools algoTutorAiTools;
    private final SuggestionGenerator suggestionGenerator;
    private final AiMessagePersister aiMessagePersister;
    private final AiLlmExecutor aiLlmExecutor;
    private final AiSseEventService aiSseEventService;
    private final AiResponseGuardrailService aiResponseGuardrailService;
    private final LessonProgressRepository lessonProgressRepository;

    @Value("${ai.history.max-messages:16}")
    private int maxHistoryMessages;

    @Value("${ai.history.max-characters:12000}")
    private int maxHistoryCharacters;

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String truncate(String value) {
        if (value == null || value.length() <= MAX_TITLE_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_TITLE_LENGTH);
    }

    public AiChatResponse chat(AiLessonChatRequest request, UUID userId) {
        LessonChatSession session = prepareLessonChatSession(request, userId);

        AiLlmExecutor.ChatResponseWithTokens llmResult = callLlmWithTokens(
                request.provider(),
                session.messages(),
                algoTutorAiTools);
        llmResult = applyResponseGuardrails(llmResult, session);

        persistConversationTurn(session.conversationId(), userId, request, llmResult);

        Boolean canAskNextHint = computeCanAskNextHintAfter(session.mode(), session.hintPolicy());

        return new AiChatResponse(
                session.conversationId(),
                llmResult.responseText(),
                session.mode().name(),
                buildQuickActions(session, request, canAskNextHint),
                Collections.emptyList(),
                canAskNextHint);
    }

    public void chatStream(AiLessonChatRequest request, UUID userId, SseEmitter emitter) {
        LessonChatSession session = prepareLessonChatSession(request, userId);
        boolean bufferForGuardrails = aiResponseGuardrailService.shouldBufferStreamingResponse(session.mode());

        streamLlmResponse(
                request.provider(),
                session.messages(),
                algoTutorAiTools,
                emitter,
                "LLM stream error",
                bufferForGuardrails,
                streamResult -> {
                    AiLlmExecutor.ChatResponseWithTokens guardedResult = applyResponseGuardrails(streamResult, session);
                    if (bufferForGuardrails) {
                        aiSseEventService.sendMessage(emitter, guardedResult.responseText());
                    }
                    persistConversationTurn(session.conversationId(), userId, request, guardedResult);

                    Boolean canAskNextHint = computeCanAskNextHintAfter(session.mode(), session.hintPolicy());

                    AiChatResponse metadata = new AiChatResponse(
                            session.conversationId(),
                            null,
                            session.mode().name(),
                            buildQuickActions(session, request, canAskNextHint),
                            Collections.emptyList(),
                            canAskNextHint);

                    aiSseEventService.sendMetadata(emitter, metadata);
                });
    }

    public AiChatResponse bootstrap(UUID userId, String lessonSlug) {
        Lesson lesson = resolveLessonBySlug(lessonSlug);
        AIConversation conversation = findOrCreateBootstrapConversation(userId, lesson);

        return new AiChatResponse(
                conversation.getId(),
                buildOnboardingMessage(lesson),
                BOOTSTRAP_MODE,
                null,
                Collections.emptyList(),
                true);
    }

    private LessonChatSession prepareLessonChatSession(AiLessonChatRequest request, UUID userId) {
        validateRequest(request);

        AiChatMode mode = parseMode(request.mode());
        LLMProvider provider = providerRouter.resolveProvider(request.provider());
        AIConversation conversation = resolveAndSaveConversation(request, userId, provider);
        LessonChatMetadata lessonMetadata = resolveLessonChatMetadata(
                conversation.getId(),
                conversation.getLessonId(),
                userId);

        enforceHintLimit(mode, lessonMetadata.hintPolicy());

        AiChatRequest chatRequest = new AiChatRequest(
                request.conversationId(), request.lessonId(), request.lessonSlug(),
                request.provider(), request.mode(), request.message(), request.code(),
                request.language(), request.judgeResult(), request.errorMessage(),
                request.failedTestCases());

        String context = aiContextService.buildContext(chatRequest);
        String systemPrompt = aiPromptService.buildSystemPrompt(mode, lessonMetadata.lessonCompleted());
        String userPrompt = aiPromptService.buildUserPrompt(chatRequest, context);

        return new LessonChatSession(
                conversation.getId(),
                mode,
                lessonMetadata.lessonType(),
                lessonMetadata.hintPolicy(),
                lessonMetadata.lessonCompleted(),
                buildNativeMessages(systemPrompt, userPrompt, conversation.getId()));
    }

    void validateRequest(AiLessonChatRequest request) {
        AiChatMode mode = parseMode(request.mode());

        if (request.message() != null && request.message().length() > MAX_MESSAGE_LENGTH) {
            throw new AppException(ErrorCode.INVALID_PAYLOAD);
        }

        if (request.code() != null && request.code().length() > MAX_CODE_LENGTH) {
            throw new AppException(ErrorCode.INVALID_PAYLOAD);
        }

        boolean messageBlank = isBlank(request.message());
        boolean codeBlank = isBlank(request.code());

        if (messageBlank && codeBlank) {
            throw new AppException(ErrorCode.INVALID_PAYLOAD);
        }

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

    private AIConversation resolveAndSaveConversation(
            AiLessonChatRequest request,
            UUID userId,
            LLMProvider provider) {
        AIConversation conversation = findOrCreateConversation(request, userId, provider);
        return conversationRepository.save(conversation);
    }

    AIConversation findOrCreateConversation(AiLessonChatRequest request, UUID userId, LLMProvider provider) {
        if (request.conversationId() != null) {
            return conversationRepository
                    .findByIdAndUserIdAndType(request.conversationId(), userId, ConversationType.LESSON)
                    .orElseThrow(() -> new AppException(ErrorCode.CONVERSATION_NOT_FOUND));
        }

        AIConversation conversation = new AIConversation();
        conversation.setUserId(userId);
        conversation.setLessonId(request.lessonId());
        conversation.setProvider(provider);
        conversation.setType(ConversationType.LESSON);
        conversation.setTitle(generateTitle(request));

        return conversation;
    }

    private String generateTitle(AiLessonChatRequest request) {
        if (!isBlank(request.message())) {
            return truncate(request.message().trim());
        }

        if (request.lessonId() == null) {
            return DEFAULT_CONVERSATION_TITLE;
        }

        return lessonRepository.findById(request.lessonId())
                .map(Lesson::getTitle)
                .filter(title -> !isBlank(title))
                .map(AiLessonChatService::truncate)
                .orElse(DEFAULT_CONVERSATION_TITLE);
    }

    private List<Message> buildNativeMessages(String systemPrompt, String userPrompt, UUID conversationId) {
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemPrompt));
        messages.addAll(getHistoryAsNativeMessages(conversationId));
        messages.add(new UserMessage(userPrompt));
        return messages;
    }

    private List<Message> getHistoryAsNativeMessages(UUID conversationId) {
        List<AiMessage> dbMessages = aiMessageRepository
                .findByConversationIdOrderByCreatedAtDesc(
                        conversationId,
                        PageRequest.of(0, Math.max(1, maxHistoryMessages)));

        if (dbMessages.isEmpty()) {
            return Collections.emptyList();
        }

        List<AiMessage> budgetedMessages = applyHistoryCharacterBudget(dbMessages);
        Collections.reverse(budgetedMessages);

        return budgetedMessages.stream()
                .flatMap(this::toNativeMessage)
                .toList();
    }

    private List<AiMessage> applyHistoryCharacterBudget(List<AiMessage> newestFirstMessages) {
        int remainingCharacters = Math.max(0, maxHistoryCharacters);
        List<AiMessage> selected = new ArrayList<>();

        for (AiMessage message : newestFirstMessages) {
            int messageSize = message.getContent() == null ? 0 : message.getContent().length();
            if (!selected.isEmpty() && messageSize > remainingCharacters) {
                break;
            }

            selected.add(message);
            remainingCharacters -= messageSize;
            if (remainingCharacters <= 0) {
                break;
            }
        }

        return selected;
    }

    private Stream<Message> toNativeMessage(AiMessage dbMessage) {
        if (dbMessage.getRole() == AiMessageRole.USER) {
            return Stream.of(new UserMessage(dbMessage.getContent()));
        }

        if (dbMessage.getRole() == AiMessageRole.ASSISTANT) {
            return Stream.of(new AssistantMessage(dbMessage.getContent()));
        }

        return Stream.empty();
    }

    AiLlmExecutor.ChatResponseWithTokens callLlmWithTokens(
            String providerName,
            List<Message> messages,
            Object tools) {
        return aiLlmExecutor.callWithFallback(providerName, messages, tools);
    }

    private void streamLlmResponse(
            String providerName,
            List<Message> messages,
            Object tools,
            SseEmitter emitter,
            String errorLogMessage,
            boolean bufferForGuardrails,
            Consumer<AiLlmExecutor.ChatResponseWithTokens> completionHandler) {
        StringBuilder guardedStreamBuffer = new StringBuilder();

        Disposable subscription = aiLlmExecutor.streamWithFallback(
                providerName,
                messages,
                tools,
                chunk -> {
                    if (bufferForGuardrails) {
                        guardedStreamBuffer.append(chunk);
                    } else {
                        aiSseEventService.sendMessage(emitter, chunk);
                    }
                },
                result -> {
                    AiLlmExecutor.ChatResponseWithTokens completedResult = bufferForGuardrails
                            ? new AiLlmExecutor.ChatResponseWithTokens(
                            guardedStreamBuffer.toString(),
                            result.inputTokens(),
                            result.outputTokens())
                            : result;
                    handleStreamCompleted(emitter, completedResult, completionHandler);
                },
                error -> aiSseEventService.completeWithError(
                        emitter,
                        errorLogMessage + " for provider [" + providerName + "]",
                        error));

        aiSseEventService.registerLifecycle(emitter, subscription);
    }

    private void handleStreamCompleted(
            SseEmitter emitter,
            AiLlmExecutor.ChatResponseWithTokens result,
            Consumer<AiLlmExecutor.ChatResponseWithTokens> completionHandler) {
        try {
            completionHandler.accept(result);
            aiSseEventService.completeSuccessfully(emitter);
        } catch (Exception e) {
            aiSseEventService.completeWithError(emitter, "Error in lesson stream completion callback", e);
        }
    }

    private void persistConversationTurn(
            UUID conversationId,
            UUID userId,
            AiLessonChatRequest request,
            AiLlmExecutor.ChatResponseWithTokens llmResult) {
        aiMessagePersister.saveMessage(
                conversationId,
                userId,
                AiMessageRole.USER,
                resolveUserMessageContent(request),
                request.mode().toUpperCase(),
                llmResult.inputTokens(),
                null);

        aiMessagePersister.saveMessage(
                conversationId,
                userId,
                AiMessageRole.ASSISTANT,
                llmResult.responseText(),
                request.mode().toUpperCase(),
                null,
                llmResult.outputTokens());
    }

    private AiLlmExecutor.ChatResponseWithTokens applyResponseGuardrails(
            AiLlmExecutor.ChatResponseWithTokens llmResult,
            LessonChatSession session) {
        String guardedText = aiResponseGuardrailService.enforceLessonDisclosurePolicy(
                llmResult.responseText(),
                session.mode(),
                session.lessonCompleted());

        return Objects.equals(guardedText, llmResult.responseText())
                ? llmResult
                : new AiLlmExecutor.ChatResponseWithTokens(
                guardedText,
                llmResult.inputTokens(),
                llmResult.outputTokens());
    }

    private String resolveUserMessageContent(AiLessonChatRequest request) {
        return !isBlank(request.message()) ? request.message() : request.code();
    }

    private LessonChatMetadata resolveLessonChatMetadata(UUID conversationId, Long lessonId, UUID userId) {
        if (lessonId == null) {
            return LessonChatMetadata.defaultMetadata();
        }

        return lessonRepository.findById(lessonId)
                .map(lesson -> toLessonChatMetadata(conversationId, lesson, userId))
                .orElseGet(LessonChatMetadata::defaultMetadata);
    }

    private LessonChatMetadata toLessonChatMetadata(UUID conversationId, Lesson lesson, UUID userId) {
        boolean lessonCompleted = lessonProgressRepository.existsByUserIdAndLessonIdAndStatus(
                userId,
                lesson.getId(),
                ProgressStatus.COMPLETED);

        if (!(lesson instanceof CodingLesson codingLesson)) {
            return new LessonChatMetadata(
                    lesson.getType(),
                    HintPolicy.notApplicable(),
                    lessonCompleted);
        }

        int maxAllowedHints = Math.min(MAX_CODING_HINTS, codingLesson.getHints().size());
        long hintMessagesCount = aiMessageRepository.countByConversationIdAndRoleAndMode(
                conversationId,
                AiMessageRole.ASSISTANT,
                HINT_MODE);

        return new LessonChatMetadata(
                lesson.getType(),
                new HintPolicy(true, maxAllowedHints, hintMessagesCount),
                lessonCompleted);
    }

    private void enforceHintLimit(AiChatMode mode, HintPolicy hintPolicy) {
        if (mode == AiChatMode.HINT && hintPolicy.applicable() && !hintPolicy.canAskNextHint()) {
            throw new AppException(ErrorCode.NO_MORE_HINTS);
        }
    }

    private Boolean computeCanAskNextHintAfter(AiChatMode mode, HintPolicy hintPolicy) {
        if (!hintPolicy.applicable()) {
            return false;
        }

        long nextHintCount = mode == AiChatMode.HINT
                ? hintPolicy.hintMessagesCount() + 1
                : hintPolicy.hintMessagesCount();

        return nextHintCount < hintPolicy.maxAllowedHints();
    }

    private List<AiQuickAction> buildQuickActions(
            LessonChatSession session,
            AiLessonChatRequest request,
            Boolean canAskNextHint) {
        return suggestionGenerator.generate(
                session.mode(),
                session.lessonType(),
                !isBlank(request.code()),
                !isBlank(request.errorMessage()),
                Boolean.TRUE.equals(canAskNextHint));
    }

    private Lesson resolveLessonBySlug(String lessonSlug) {
        return lessonRepository.findBySlug(lessonSlug)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));
    }

    private AIConversation findOrCreateBootstrapConversation(UUID userId, Lesson lesson) {
        if (lesson != null && lesson.getId() != null) {
            return conversationRepository
                    .findTopByUserIdAndLessonIdAndTypeOrderByUpdatedAtDesc(userId, lesson.getId(),
                            ConversationType.LESSON)
                    .orElseGet(() -> createBootstrapConversation(userId, lesson));
        }

        AIConversation conversation = new AIConversation();
        conversation.setUserId(userId);
        conversation.setTitle(DEFAULT_AI_ASSISTANT_TITLE);
        conversation.setType(ConversationType.LESSON);

        return conversationRepository.save(conversation);
    }

    private AIConversation createBootstrapConversation(UUID userId, Lesson lesson) {
        AIConversation conversation = new AIConversation();
        conversation.setUserId(userId);
        conversation.setLessonId(lesson.getId());
        conversation.setProvider(LLMProvider.OPENAI);
        conversation.setType(ConversationType.LESSON);
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
        return isBlank(title) ? "bài học này" : truncate(title);
    }

    private record LessonChatSession(
            UUID conversationId,
            AiChatMode mode,
            LessonType lessonType,
            HintPolicy hintPolicy,
            boolean lessonCompleted,
            List<Message> messages) {
    }

    private record LessonChatMetadata(
            LessonType lessonType,
            HintPolicy hintPolicy,
            boolean lessonCompleted) {
        static LessonChatMetadata defaultMetadata() {
            return new LessonChatMetadata(LessonType.THEORY, HintPolicy.notApplicable(), false);
        }
    }

    private record HintPolicy(
            boolean applicable,
            int maxAllowedHints,
            long hintMessagesCount) {
        static HintPolicy notApplicable() {
            return new HintPolicy(false, 0, 0);
        }

        boolean canAskNextHint() {
            return hintMessagesCount < maxAllowedHints;
        }
    }

}

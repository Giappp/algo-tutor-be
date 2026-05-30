package org.rap.algotutorbe.ai.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rap.algotutorbe.ai.dto.*;
import org.rap.algotutorbe.ai.dto.AiRoadmapAdvisoryResponse.RoadmapInfo;
import org.rap.algotutorbe.ai.entity.AIConversation;
import org.rap.algotutorbe.ai.entity.AiMessage;
import org.rap.algotutorbe.ai.enums.AiChatMode;
import org.rap.algotutorbe.ai.enums.AiMessageRole;
import org.rap.algotutorbe.ai.enums.LLMProvider;
import org.rap.algotutorbe.ai.repository.AiMessageRepository;
import org.rap.algotutorbe.ai.repository.ConversationRepository;
import org.rap.algotutorbe.ai.tools.AlgoTutorAiTools;
import org.rap.algotutorbe.ai.tools.RoadmapAdvisoryTools;
import org.rap.algotutorbe.common.errors.ErrorCode;
import org.rap.algotutorbe.common.exception.AppException;
import org.rap.algotutorbe.learning.enums.LessonType;
import org.rap.algotutorbe.learning.models.CodingLesson;
import org.rap.algotutorbe.learning.models.LearningPath;
import org.rap.algotutorbe.learning.models.Lesson;
import org.rap.algotutorbe.learning.repositories.LearningPathRepository;
import org.rap.algotutorbe.learning.repositories.LessonRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.net.SocketTimeoutException;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatService {

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
    private final LearningPathRepository learningPathRepository;

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String truncate(String value) {
        if (value == null || value.length() <= AiChatService.MAX_TITLE_LENGTH) {
            return value;
        }
        return value.substring(0, AiChatService.MAX_TITLE_LENGTH);
    }

    public AiChatResponse chat(AiChatRequest request, UUID userId) {
        LessonChatSession session = prepareLessonChatSession(request, userId);

        ChatResponseWithTokens llmResult = callLlmWithTokens(
                request.provider(),
                session.messages(),
                algoTutorAiTools);

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

    private String buildHistoryContext(UUID conversationId) {
        List<AiMessage> dbMessages = aiMessageRepository
                .findTop10ByConversationIdOrderByCreatedAtDesc(conversationId);

        if (dbMessages.isEmpty()) {
            return "No previous messages.";
        }

        Collections.reverse(dbMessages);

        StringBuilder builder = new StringBuilder();

        for (AiMessage message : dbMessages) {
            String role = message.getRole() == AiMessageRole.USER ? "User" : "Assistant";

            builder.append(role)
                    .append(": ")
                    .append(message.getContent())
                    .append("\n");
        }

        return builder.toString();
    }

    public void chatStream(AiChatRequest request, UUID userId, SseEmitter emitter) {
        LessonChatSession session = prepareLessonChatSession(request, userId);

        streamLlmResponse(
                request.provider(),
                session.messages(),
                algoTutorAiTools,
                emitter,
                "LLM stream error",
                streamResult -> {
                    persistConversationTurn(session.conversationId(), userId, request, streamResult);

                    Boolean canAskNextHint = computeCanAskNextHintAfter(session.mode(), session.hintPolicy());

                    AiChatResponse metadata = new AiChatResponse(
                            session.conversationId(),
                            null,
                            session.mode().name(),
                            buildQuickActions(session, request, canAskNextHint),
                            Collections.emptyList(),
                            canAskNextHint);

                    sendSseEvent(emitter, "metadata", metadata);
                });
    }

    public AiGeneralChatResponse generalChat(
            AiChatRequest request,
            UUID userId,
            List<RoadmapInfo> availableRoadmaps) {
        GeneralChatSession session = prepareGeneralChatSession(request, userId, availableRoadmaps);

        ChatResponseWithTokens llmResult = callLlmWithTokens(
                request.provider(),
                session.messages(),
                session.advisoryTools());

        persistConversationTurn(session.conversationId(), userId, request, llmResult);

        return new AiGeneralChatResponse(
                session.conversationId(),
                llmResult.responseText(),
                resolveRecommendedRoadmaps(
                        session.advisoryTools().getRecommendedSlugs(),
                        availableRoadmaps,
                        llmResult.responseText()));
    }

    public void generalChatStream(
            AiChatRequest request,
            UUID userId,
            SseEmitter emitter,
            List<RoadmapInfo> availableRoadmaps) {
        try {
            GeneralChatSession session = prepareGeneralChatSession(request, userId, availableRoadmaps);

            ChatResponseWithTokens llmResult = callLlmWithTokens(
                    request.provider(),
                    session.messages(),
                    session.advisoryTools());

            ChatResponseWithTokens normalizedResult = new ChatResponseWithTokens(
                    normalizeAiText(llmResult.responseText()),
                    llmResult.inputTokens(),
                    llmResult.outputTokens());

            persistConversationTurn(session.conversationId(), userId, request, normalizedResult);

            sendSseEvent(emitter, "message", new AiChunkResponse(normalizedResult.responseText()));

            AiRoadmapAdvisoryResponse metadata = new AiRoadmapAdvisoryResponse(
                    session.conversationId(),
                    resolveRecommendedRoadmaps(
                            session.advisoryTools().getRecommendedSlugs(),
                            availableRoadmaps,
                            normalizedResult.responseText()));

            sendSseEvent(emitter, "metadata", metadata);
            emitter.complete();
        } catch (Exception e) {
            log.error("General LLM stream error", e);
            emitter.completeWithError(new AppException(ErrorCode.AI_SERVICE_UNAVAILABLE, e));
        }
    }

    private String normalizeAiText(String text) {
        if (text == null) {
            return null;
        }

        return text
                .replace("\r\n", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
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

    @Transactional(readOnly = true)
    public List<RoadmapInfo> getRoadmapsForAdvisory() {
        return learningPathRepository.findByDeletedFalseAndIsPublishedTrue()
                .stream()
                .map(this::toRoadmapInfo)
                .toList();
    }

    private LessonChatSession prepareLessonChatSession(AiChatRequest request, UUID userId) {
        validateRequest(request);

        AiChatMode mode = parseMode(request.mode());
        LLMProvider provider = providerRouter.resolveProvider(request.provider());
        AIConversation conversation = resolveAndSaveConversation(request, userId, provider);
        LessonChatMetadata lessonMetadata = resolveLessonChatMetadata(
                conversation.getId(),
                conversation.getLessonId());

        enforceHintLimit(mode, lessonMetadata.hintPolicy());

        String systemPrompt = aiPromptService.buildSystemPrompt(mode);
        String userPrompt = aiPromptService.buildUserPrompt(request, aiContextService.buildContext(request), "");

        return new LessonChatSession(
                conversation.getId(),
                mode,
                lessonMetadata.lessonType(),
                lessonMetadata.hintPolicy(),
                buildNativeMessages(systemPrompt, userPrompt, conversation.getId()));
    }

    private GeneralChatSession prepareGeneralChatSession(
            AiChatRequest request,
            UUID userId,
            List<RoadmapInfo> availableRoadmaps) {

        validateGeneralChatRequest(request);

        LLMProvider provider = providerRouter.resolveProvider(request.provider());
        AIConversation conversation = resolveAndSaveConversation(request, userId, provider);

        String systemPrompt = aiPromptService.buildGeneralSystemPrompt();
        String historyContext = buildHistoryContext(conversation.getId());

        String userPrompt = """
                Conversation history:
                %s
                
                Current user message:
                %s
                """.formatted(
                historyContext,
                aiPromptService.buildUserPrompt(request, "", ""));

        RoadmapAdvisoryTools advisoryTools = new RoadmapAdvisoryTools(availableRoadmaps);

        return new GeneralChatSession(
                conversation.getId(),
                List.of(
                        new SystemMessage(systemPrompt),
                        new UserMessage(userPrompt)
                ),
                advisoryTools);
    }

    private void validateGeneralChatRequest(AiChatRequest request) {
        if (isBlank(request.message())) {
            throw new AppException(ErrorCode.INVALID_PAYLOAD);
        }

        if (request.message().length() > MAX_MESSAGE_LENGTH) {
            throw new AppException(ErrorCode.INVALID_PAYLOAD);
        }
    }

    void validateRequest(AiChatRequest request) {
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
            AiChatRequest request,
            UUID userId,
            LLMProvider provider) {
        AIConversation conversation = findOrCreateConversation(request, userId, provider);
        return conversationRepository.save(conversation);
    }

    AIConversation findOrCreateConversation(AiChatRequest request, UUID userId, LLMProvider provider) {
        if (request.conversationId() != null) {
            return conversationRepository.findByIdAndUserId(request.conversationId(), userId)
                    .orElseThrow(() -> new AppException(ErrorCode.CONVERSATION_NOT_FOUND));
        }

        AIConversation conversation = new AIConversation();
        conversation.setUserId(userId);
        conversation.setLessonId(request.lessonId());
        conversation.setProvider(provider);
        conversation.setTitle(generateTitle(request));

        return conversation;
    }

    private String generateTitle(AiChatRequest request) {
        if (!isBlank(request.message())) {
            return truncate(request.message().trim());
        }

        if (request.lessonId() == null) {
            return DEFAULT_CONVERSATION_TITLE;
        }

        return lessonRepository.findById(request.lessonId())
                .map(Lesson::getTitle)
                .filter(title -> !isBlank(title))
                .map(AiChatService::truncate)
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
                .findTop10ByConversationIdOrderByCreatedAtDesc(conversationId);

        if (dbMessages.isEmpty()) {
            return Collections.emptyList();
        }

        Collections.reverse(dbMessages);

        return dbMessages.stream()
                .flatMap(this::toNativeMessage)
                .toList();
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

    ChatResponseWithTokens callLlmWithTokens(
            String providerName,
            List<Message> messages,
            Object tools) {
        try {
            ChatResponse chatResponse = providerRouter.route(providerName)
                    .prompt()
                    .messages(messages)
                    .tools(tools)
                    .call()
                    .chatResponse();

            String responseText = extractRequiredResponseText(chatResponse);
            TokenUsage tokenUsage = extractTokenUsage(chatResponse);

            return new ChatResponseWithTokens(
                    responseText,
                    tokenUsage.inputTokens(),
                    tokenUsage.outputTokens());
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            logLlmException(providerName, e);
            throw new AppException(ErrorCode.AI_SERVICE_UNAVAILABLE, e);
        }
    }

    private void streamLlmResponse(
            String providerName,
            List<Message> messages,
            Object tools,
            SseEmitter emitter,
            String errorLogMessage,
            StreamCompletionHandler completionHandler) {
        try {
            ChatClient chatClient = providerRouter.route(providerName);
            Flux<ChatResponse> stream = chatClient.prompt()
                    .messages(messages)
                    .tools(tools)
                    .stream()
                    .chatResponse();

            StreamAccumulator accumulator = new StreamAccumulator();

            Disposable subscription = stream.subscribe(
                    chatResponse -> handleStreamChunk(chatResponse, accumulator, emitter),
                    error -> handleStreamError(providerName, emitter, errorLogMessage, error),
                    () -> handleStreamCompleted(emitter, accumulator, completionHandler));

            emitter.onCompletion(subscription::dispose);
            emitter.onTimeout(subscription::dispose);
            emitter.onError(e -> subscription.dispose());
        } catch (Exception e) {
            log.error("Failed to initiate LLM stream for provider [{}]", providerName, e);
            emitter.completeWithError(new AppException(ErrorCode.AI_SERVICE_UNAVAILABLE, e));
        }
    }

    private void handleStreamChunk(
            ChatResponse chatResponse,
            StreamAccumulator accumulator,
            SseEmitter emitter) {
        if (chatResponse == null) {
            return;
        }

        accumulator.updateUsage(extractTokenUsage(chatResponse));

        String chunkText = extractNullableResponseText(chatResponse);
        if (chunkText == null) {
            return;
        }

        accumulator.append(chunkText);
        sendSseEvent(emitter, "message", new AiChunkResponse(chunkText));
    }

    private void handleStreamError(
            String providerName,
            SseEmitter emitter,
            String errorLogMessage,
            Throwable error) {
        log.error("{} for provider [{}]", errorLogMessage, providerName, error);
        emitter.completeWithError(new AppException(ErrorCode.AI_SERVICE_UNAVAILABLE, error));
    }

    private void handleStreamCompleted(
            SseEmitter emitter,
            StreamAccumulator accumulator,
            StreamCompletionHandler completionHandler) {
        try {
            ChatResponseWithTokens result = accumulator.toChatResponseWithTokens();
            completionHandler.onCompleted(result);
            emitter.complete();
        } catch (Exception e) {
            log.error("Error in stream completion callback", e);
            emitter.completeWithError(e);
        }
    }

    private String extractRequiredResponseText(ChatResponse chatResponse) {
        String responseText = extractNullableResponseText(chatResponse);

        if (isBlank(responseText)) {
            throw new AppException(ErrorCode.AI_SERVICE_UNAVAILABLE);
        }

        return responseText;
    }

    private String extractNullableResponseText(ChatResponse chatResponse) {
        if (chatResponse == null) {
            return null;
        } else {
            chatResponse.getResult();
        }

        return chatResponse.getResult().getOutput().getText();
    }

    private TokenUsage extractTokenUsage(ChatResponse chatResponse) {
        if (chatResponse == null || chatResponse.getMetadata().getUsage() == null) {
            return TokenUsage.empty();
        }

        Usage usage = chatResponse.getMetadata().getUsage();
        return new TokenUsage(usage.getPromptTokens(), usage.getCompletionTokens());
    }

    private void persistConversationTurn(
            UUID conversationId,
            UUID userId,
            AiChatRequest request,
            ChatResponseWithTokens llmResult) {
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

    private String resolveUserMessageContent(AiChatRequest request) {
        return !isBlank(request.message()) ? request.message() : request.code();
    }

    private LessonChatMetadata resolveLessonChatMetadata(UUID conversationId, Long lessonId) {
        if (lessonId == null) {
            return LessonChatMetadata.defaultMetadata();
        }

        return lessonRepository.findById(lessonId)
                .map(lesson -> toLessonChatMetadata(conversationId, lesson))
                .orElseGet(LessonChatMetadata::defaultMetadata);
    }

    private LessonChatMetadata toLessonChatMetadata(UUID conversationId, Lesson lesson) {
        if (!(lesson instanceof CodingLesson codingLesson)) {
            return new LessonChatMetadata(
                    lesson.getType(),
                    HintPolicy.notApplicable());
        }

        int maxAllowedHints = Math.min(MAX_CODING_HINTS, codingLesson.getHints().size());
        long hintMessagesCount = aiMessageRepository.countByConversationIdAndRoleAndMode(
                conversationId,
                AiMessageRole.ASSISTANT,
                HINT_MODE);

        return new LessonChatMetadata(
                lesson.getType(),
                new HintPolicy(true, maxAllowedHints, hintMessagesCount));
    }

    private void enforceHintLimit(AiChatMode mode, HintPolicy hintPolicy) {
        if (mode == AiChatMode.HINT && hintPolicy.applicable() && !hintPolicy.canAskNextHint()) {
            throw new AppException(ErrorCode.NO_MORE_HINTS);
        }
    }

    private Boolean computeCanAskNextHintAfter(AiChatMode mode, HintPolicy hintPolicy) {
        if (!hintPolicy.applicable()) {
            return null;
        }

        long nextHintCount = mode == AiChatMode.HINT
                ? hintPolicy.hintMessagesCount() + 1
                : hintPolicy.hintMessagesCount();

        return nextHintCount < hintPolicy.maxAllowedHints();
    }

    private List<AiQuickAction> buildQuickActions(
            LessonChatSession session,
            AiChatRequest request,
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
                    .findTopByUserIdAndLessonIdOrderByUpdatedAtDesc(userId, lesson.getId())
                    .orElseGet(() -> createBootstrapConversation(userId, lesson));
        }

        AIConversation conversation = new AIConversation();
        conversation.setUserId(userId);
        conversation.setTitle(DEFAULT_AI_ASSISTANT_TITLE);

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
        return isBlank(title) ? "bài học này" : truncate(title);
    }

    private RoadmapInfo toRoadmapInfo(LearningPath learningPath) {
        return new RoadmapInfo(
                learningPath.getName(),
                learningPath.getSlug(),
                learningPath.getLevel() != null ? learningPath.getLevel().name() : null,
                learningPath.getDescription(),
                learningPath.getThumbnailUrl(),
                countTopics(learningPath),
                countLessons(learningPath),
                Boolean.TRUE.equals(learningPath.getIsPremium()));
    }

    private int countTopics(LearningPath learningPath) {
        return learningPath.getTopics() == null ? 0 : learningPath.getTopics().size();
    }

    private int countLessons(LearningPath learningPath) {
        if (learningPath.getTopics() == null) {
            return 0;
        }

        return learningPath.getTopics().stream()
                .filter(topic -> topic.getLessons() != null)
                .mapToInt(topic -> topic.getLessons().size())
                .sum();
    }

    private List<RoadmapInfo> resolveRecommendedRoadmaps(
            List<String> recommendedSlugs,
            List<RoadmapInfo> availableRoadmaps,
            String answer) {
        Map<String, RoadmapInfo> result = new LinkedHashMap<>();

        recommendedSlugs.stream()
                .filter(slug -> !isBlank(slug))
                .forEach(slug -> findRoadmapBySlug(availableRoadmaps, slug)
                        .ifPresent(roadmap -> result.putIfAbsent(roadmap.slug(), roadmap)));

        if (result.isEmpty()) {
            addRoadmapsMentionedInAnswer(result, availableRoadmaps, answer);
        }

        return new ArrayList<>(result.values());
    }

    private java.util.Optional<RoadmapInfo> findRoadmapBySlug(
            List<RoadmapInfo> roadmaps,
            String slug) {
        return roadmaps.stream()
                .filter(roadmap -> roadmap.slug().equalsIgnoreCase(slug))
                .findFirst();
    }

    private void addRoadmapsMentionedInAnswer(
            Map<String, RoadmapInfo> result,
            List<RoadmapInfo> availableRoadmaps,
            String answer) {
        if (isBlank(answer)) {
            return;
        }

        String normalizedAnswer = answer.toLowerCase();

        availableRoadmaps.stream()
                .filter(roadmap -> containsRoadmapReference(normalizedAnswer, roadmap))
                .forEach(roadmap -> result.putIfAbsent(roadmap.slug(), roadmap));
    }

    private boolean containsRoadmapReference(String normalizedAnswer, RoadmapInfo roadmap) {
        return normalizedAnswer.contains(roadmap.slug().toLowerCase())
                || normalizedAnswer.contains(roadmap.name().toLowerCase());
    }

    private void sendSseEvent(SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
        } catch (Exception e) {
            log.error("Failed to send SSE event [{}]", eventName, e);
        }
    }

    private void logLlmException(String providerName, Exception e) {
        if (isTimeoutException(e)) {
            log.error("LLM call timed out for provider [{}]", providerName, e);
            return;
        }

        log.error("LLM provider error for provider [{}]", providerName, e);
    }

    private boolean isTimeoutException(Exception e) {
        Throwable current = e;

        while (current != null) {
            if (current instanceof SocketTimeoutException || current instanceof TimeoutException) {
                return true;
            }

            String message = current.getMessage();
            if (message != null && isTimeoutMessage(message)) {
                return true;
            }

            current = current.getCause();
        }

        return false;
    }

    private boolean isTimeoutMessage(String message) {
        String normalizedMessage = message.toLowerCase();
        return normalizedMessage.contains("timed out")
                || normalizedMessage.contains("timeout")
                || normalizedMessage.contains("read timed out");
    }

    private interface StreamCompletionHandler {
        void onCompleted(ChatResponseWithTokens result);
    }

    private record LessonChatSession(
            UUID conversationId,
            AiChatMode mode,
            LessonType lessonType,
            HintPolicy hintPolicy,
            List<Message> messages) {
    }

    private record GeneralChatSession(
            UUID conversationId,
            List<Message> messages,
            RoadmapAdvisoryTools advisoryTools) {
    }

    private record LessonChatMetadata(
            LessonType lessonType,
            HintPolicy hintPolicy) {
        static LessonChatMetadata defaultMetadata() {
            return new LessonChatMetadata(LessonType.THEORY, HintPolicy.notApplicable());
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

    private record TokenUsage(Integer inputTokens, Integer outputTokens) {
        static TokenUsage empty() {
            return new TokenUsage(null, null);
        }
    }

    record ChatResponseWithTokens(String responseText, Integer inputTokens, Integer outputTokens) {
    }

    private static final class StreamAccumulator {
        private final StringBuilder answer = new StringBuilder();
        private final AtomicInteger inputTokens = new AtomicInteger(0);
        private final AtomicInteger outputTokens = new AtomicInteger(0);

        void append(String chunkText) {
            answer.append(chunkText);
        }

        void updateUsage(TokenUsage usage) {
            if (usage.inputTokens() != null) {
                inputTokens.set(usage.inputTokens());
            }

            if (usage.outputTokens() != null) {
                outputTokens.set(usage.outputTokens());
            }
        }

        ChatResponseWithTokens toChatResponseWithTokens() {
            return new ChatResponseWithTokens(
                    answer.toString(),
                    inputTokens.get() > 0 ? inputTokens.get() : null,
                    outputTokens.get() > 0 ? outputTokens.get() : null);
        }
    }
}
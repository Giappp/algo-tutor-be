package org.rap.algotutorbe.ai.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rap.algotutorbe.ai.dto.AiChunkResponse;
import org.rap.algotutorbe.ai.dto.AiGeneralChatRequest;
import org.rap.algotutorbe.ai.dto.AiGeneralChatResponse;
import org.rap.algotutorbe.ai.dto.AiRoadmapAdvisoryResponse;
import org.rap.algotutorbe.ai.dto.AiRoadmapAdvisoryResponse.RoadmapInfo;
import org.rap.algotutorbe.ai.entity.AIConversation;
import org.rap.algotutorbe.ai.entity.AiMessage;
import org.rap.algotutorbe.ai.enums.AiMessageRole;
import org.rap.algotutorbe.ai.enums.ConversationType;
import org.rap.algotutorbe.ai.enums.LLMProvider;
import org.rap.algotutorbe.ai.repository.AiMessageRepository;
import org.rap.algotutorbe.ai.repository.ConversationRepository;
import org.rap.algotutorbe.ai.tools.RoadmapAdvisoryTools;
import org.rap.algotutorbe.common.errors.ErrorCode;
import org.rap.algotutorbe.common.exception.AppException;
import org.rap.algotutorbe.learning.models.LearningPath;
import org.rap.algotutorbe.learning.repositories.LearningPathRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.net.SocketTimeoutException;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiGeneralChatService {

    private static final int MAX_MESSAGE_LENGTH = 5_000;
    private static final int MAX_TITLE_LENGTH = 255;
    private static final String DEFAULT_CONVERSATION_TITLE = "New Conversation";

    private static final String GENERAL_ASSISTANT_SYSTEM_PROMPT = """
            Bạn là Trợ lý Học tập trực thuộc nền tảng học lập trình AlgoTutor.
            
            Nhiệm vụ của bạn:
            - Trả lời các câu hỏi chào hỏi, tự giới thiệu, và giải đáp các thắc mắc chung về lập trình, giải thuật của học viên một cách chính xác, ngắn gọn và hữu ích.
            - Phản hồi bằng Tiếng Việt tự nhiên, thân thiện và kiên nhẫn.
            - Trình bày câu trả lời đẹp mắt bằng định dạng Markdown (tiêu đề, bôi đậm, list).
            - Nếu học viên có nhu cầu muốn tư vấn lộ trình học tập, hãy nhắc nhở hoặc gợi ý họ hỏi về lộ trình (Ví dụ: "Bạn có thể hỏi tôi về lộ trình học cấu trúc dữ liệu, giải thuật, Java, Python,... để tôi giới thiệu chi tiết nhất!").
            """;

    private final AiPromptService aiPromptService;
    private final ConversationRepository conversationRepository;
    private final ProviderRouter providerRouter;
    private final AiMessageRepository aiMessageRepository;
    private final AiMessagePersister aiMessagePersister;
    private final LearningPathRepository learningPathRepository;

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String truncate(String value) {
        if (value == null || value.length() <= MAX_TITLE_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_TITLE_LENGTH);
    }

    public AiGeneralChatResponse generalChat(
            AiGeneralChatRequest request,
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
                        session.advisoryTools() != null ? session.advisoryTools().getRecommendedSlugs() : Collections.emptyList(),
                        availableRoadmaps,
                        llmResult.responseText()));
    }

    public void generalChatStream(
            AiGeneralChatRequest request,
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
                            session.advisoryTools() != null ? session.advisoryTools().getRecommendedSlugs() : Collections.emptyList(),
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

    @Transactional(readOnly = true)
    public List<RoadmapInfo> getRoadmapsForAdvisory() {
        return learningPathRepository.findByDeletedFalseAndIsPublishedTrue()
                .stream()
                .map(this::toRoadmapInfo)
                .toList();
    }

    private GeneralChatSession prepareGeneralChatSession(
            AiGeneralChatRequest request,
            UUID userId,
            List<RoadmapInfo> availableRoadmaps) {

        validateGeneralChatRequest(request);

        LLMProvider provider = providerRouter.resolveProvider(request.provider());
        AIConversation conversation = resolveAndSaveConversation(request, userId, provider);

        boolean roadmapIntent = isRoadmapAdvisoryIntent(request.message(), conversation);

        String systemPrompt = roadmapIntent
                ? aiPromptService.buildGeneralSystemPrompt()
                : GENERAL_ASSISTANT_SYSTEM_PROMPT;

        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemPrompt));
        messages.addAll(getHistoryAsNativeMessages(conversation.getId()));
        messages.add(new UserMessage(request.message()));

        RoadmapAdvisoryTools advisoryTools = roadmapIntent
                ? new RoadmapAdvisoryTools(availableRoadmaps)
                : null;

        return new GeneralChatSession(
                conversation.getId(),
                messages,
                advisoryTools);
    }

    private boolean isRoadmapAdvisoryIntent(String message, AIConversation conversation) {
        if (isRoadmapAdvisoryKeyword(message)) {
            return true;
        }
        return conversation != null && isRoadmapAdvisoryKeyword(conversation.getTitle());
    }

    private boolean isRoadmapAdvisoryKeyword(String text) {
        if (isBlank(text)) {
            return false;
        }
        String lower = text.toLowerCase();
        return lower.contains("lộ trình")
                || lower.contains("roadmap")
                || lower.contains("học gì")
                || lower.contains("bắt đầu")
                || lower.contains("định hướng")
                || lower.contains("tư vấn")
                || lower.contains("khóa học")
                || lower.contains("course")
                || lower.contains("learning path")
                || lower.contains("recommend")
                || lower.contains("gợi ý")
                || lower.contains("chọn");
    }

    private void validateGeneralChatRequest(AiGeneralChatRequest request) {
        if (isBlank(request.message())) {
            throw new AppException(ErrorCode.INVALID_PAYLOAD);
        }

        if (request.message().length() > MAX_MESSAGE_LENGTH) {
            throw new AppException(ErrorCode.INVALID_PAYLOAD);
        }
    }

    private AIConversation resolveAndSaveConversation(
            AiGeneralChatRequest request,
            UUID userId,
            LLMProvider provider) {
        AIConversation conversation = findOrCreateConversation(request, userId, provider);
        return conversationRepository.save(conversation);
    }

    AIConversation findOrCreateConversation(AiGeneralChatRequest request, UUID userId, LLMProvider provider) {
        if (request.conversationId() != null) {
            return conversationRepository
                    .findByIdAndUserIdAndType(request.conversationId(), userId, ConversationType.GENERAL)
                    .orElseThrow(() -> new AppException(ErrorCode.CONVERSATION_NOT_FOUND));
        }

        AIConversation conversation = new AIConversation();
        conversation.setUserId(userId);
        conversation.setProvider(provider);
        conversation.setType(ConversationType.GENERAL);
        conversation.setTitle(generateTitle(request));

        return conversation;
    }

    private String generateTitle(AiGeneralChatRequest request) {
        if (!isBlank(request.message())) {
            return truncate(request.message().trim());
        }
        return DEFAULT_CONVERSATION_TITLE;
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
            var promptSpec = providerRouter.route(providerName)
                    .prompt()
                    .messages(messages);

            if (tools != null) {
                promptSpec = promptSpec.tools(tools);
            }

            ChatResponse chatResponse = promptSpec
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
            AiGeneralChatRequest request,
            ChatResponseWithTokens llmResult) {
        aiMessagePersister.saveMessage(
                conversationId,
                userId,
                AiMessageRole.USER,
                request.message(),
                "GENERAL",
                llmResult.inputTokens(),
                null);

        aiMessagePersister.saveMessage(
                conversationId,
                userId,
                AiMessageRole.ASSISTANT,
                llmResult.responseText(),
                "GENERAL",
                null,
                llmResult.outputTokens());
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

    private record GeneralChatSession(
            UUID conversationId,
            List<Message> messages,
            RoadmapAdvisoryTools advisoryTools) {
    }

    private record TokenUsage(Integer inputTokens, Integer outputTokens) {
        static TokenUsage empty() {
            return new TokenUsage(null, null);
        }
    }

    record ChatResponseWithTokens(String responseText, Integer inputTokens, Integer outputTokens) {
    }
}

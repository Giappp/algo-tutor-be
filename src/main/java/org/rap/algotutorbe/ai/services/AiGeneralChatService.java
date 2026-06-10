package org.rap.algotutorbe.ai.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rap.algotutorbe.ai.dto.AiGeneralChatRequest;
import org.rap.algotutorbe.ai.dto.AiGeneralChatResponse;
import org.rap.algotutorbe.ai.dto.AiRoadmapAdvisoryResponse;
import org.rap.algotutorbe.ai.dto.AiRoadmapAdvisoryResponse.RoadmapInfo;
import org.rap.algotutorbe.ai.entity.AIConversation;
import org.rap.algotutorbe.ai.entity.AiMessage;
import org.rap.algotutorbe.ai.enums.AiGeneralChatIntent;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;

import java.util.*;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiGeneralChatService {

    private static final int MAX_MESSAGE_LENGTH = 5_000;
    private static final int MAX_TITLE_LENGTH = 255;
    private static final String DEFAULT_CONVERSATION_TITLE = "New Conversation";

    private final AiPromptService aiPromptService;
    private final ConversationRepository conversationRepository;
    private final ProviderRouter providerRouter;
    private final AiMessageRepository aiMessageRepository;
    private final AiMessagePersister aiMessagePersister;
    private final LearningPathRepository learningPathRepository;
    private final AiLlmExecutor aiLlmExecutor;
    private final AiSseEventService aiSseEventService;
    private final AiGeneralChatIntentClassifier intentClassifier;

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

    public AiGeneralChatResponse generalChat(
            AiGeneralChatRequest request,
            UUID userId,
            List<RoadmapInfo> availableRoadmaps) {
        GeneralChatSession session = prepareGeneralChatSession(request, userId, availableRoadmaps);

        AiLlmExecutor.ChatResponseWithTokens llmResult = callLlmWithTokens(
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
        GeneralChatSession session = prepareGeneralChatSession(request, userId, availableRoadmaps);

        Disposable subscription = aiLlmExecutor.streamWithFallback(
                request.provider(),
                session.messages(),
                session.advisoryTools(),
                chunk -> aiSseEventService.sendMessage(emitter, chunk),
                llmResult -> handleGeneralStreamCompleted(
                        emitter,
                        session,
                        userId,
                        request,
                        availableRoadmaps,
                        llmResult),
                error -> aiSseEventService.completeWithError(emitter, "General LLM stream error", error));

        aiSseEventService.registerLifecycle(emitter, subscription);
    }

    private void handleGeneralStreamCompleted(
            SseEmitter emitter,
            GeneralChatSession session,
            UUID userId,
            AiGeneralChatRequest request,
            List<RoadmapInfo> availableRoadmaps,
            AiLlmExecutor.ChatResponseWithTokens llmResult) {
        try {
            AiLlmExecutor.ChatResponseWithTokens normalizedResult = new AiLlmExecutor.ChatResponseWithTokens(
                    normalizeAiText(llmResult.responseText()),
                    llmResult.inputTokens(),
                    llmResult.outputTokens());

            persistConversationTurn(session.conversationId(), userId, request, normalizedResult);

            AiRoadmapAdvisoryResponse metadata = new AiRoadmapAdvisoryResponse(
                    session.conversationId(),
                    resolveRecommendedRoadmaps(
                            session.advisoryTools() != null ? session.advisoryTools().getRecommendedSlugs() : Collections.emptyList(),
                            availableRoadmaps,
                            normalizedResult.responseText()));

            aiSseEventService.sendMetadata(emitter, metadata);
            aiSseEventService.completeSuccessfully(emitter);
        } catch (Exception e) {
            aiSseEventService.completeWithError(emitter, "Error in general stream completion callback", e);
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

        AiGeneralChatIntent intent = intentClassifier.classify(request.message(), conversation);

        String systemPrompt = intent == AiGeneralChatIntent.ROADMAP_ADVISORY
                ? aiPromptService.buildGeneralSystemPrompt()
                : aiPromptService.buildGeneralAssistantSystemPrompt(intent);

        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemPrompt));
        messages.addAll(getHistoryAsNativeMessages(conversation.getId()));
        messages.add(new UserMessage(request.message()));

        RoadmapAdvisoryTools advisoryTools = intent == AiGeneralChatIntent.ROADMAP_ADVISORY
                ? new RoadmapAdvisoryTools(availableRoadmaps)
                : null;

        return new GeneralChatSession(
                conversation.getId(),
                messages,
                advisoryTools);
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

    private void persistConversationTurn(
            UUID conversationId,
            UUID userId,
            AiGeneralChatRequest request,
            AiLlmExecutor.ChatResponseWithTokens llmResult) {
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

}

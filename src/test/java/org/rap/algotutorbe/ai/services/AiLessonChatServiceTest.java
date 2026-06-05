package org.rap.algotutorbe.ai.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rap.algotutorbe.ai.dto.AiChatResponse;
import org.rap.algotutorbe.ai.dto.AiLessonChatRequest;
import org.rap.algotutorbe.ai.entity.AIConversation;
import org.rap.algotutorbe.ai.enums.AiChatMode;
import org.rap.algotutorbe.ai.enums.AiMessageRole;
import org.rap.algotutorbe.ai.enums.ConversationType;
import org.rap.algotutorbe.ai.repository.AiMessageRepository;
import org.rap.algotutorbe.ai.repository.ConversationRepository;
import org.rap.algotutorbe.ai.tools.AlgoTutorAiTools;
import org.rap.algotutorbe.common.errors.ErrorCode;
import org.rap.algotutorbe.common.exception.AppException;
import org.rap.algotutorbe.learning.models.CodingLesson;
import org.rap.algotutorbe.learning.repositories.LessonProgressRepository;
import org.rap.algotutorbe.learning.repositories.LessonRepository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiLessonChatServiceTest {

    @Mock
    private AiContextService aiContextService;
    @Mock
    private AiPromptService aiPromptService;
    @Mock
    private ConversationRepository conversationRepository;
    @Mock
    private ProviderRouter providerRouter;
    @Mock
    private LessonRepository lessonRepository;
    @Mock
    private AiMessageRepository aiMessageRepository;
    @Mock
    private AlgoTutorAiTools algoTutorAiTools;
    @Mock
    private SuggestionGenerator suggestionGenerator;
    @Mock
    private AiMessagePersister aiMessagePersister;
    @Mock
    private AiLlmExecutor aiLlmExecutor;
    @Mock
    private AiResponseGuardrailService aiResponseGuardrailService;
    @Mock
    private LessonProgressRepository lessonProgressRepository;

    @InjectMocks
    @Spy
    private AiLessonChatService aiLessonChatService;

    @Test
    void validateRequest_shouldThrowExceptionWhenModeInvalid() {
        AiLessonChatRequest request = new AiLessonChatRequest(
                null, 1L, "slug", "GEMINI", "INVALID_MODE", "message", null, null, null, null,
                Collections.emptyList());

        assertThatThrownBy(() -> aiLessonChatService.chat(request, UUID.randomUUID()))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("error", ErrorCode.INVALID_CHAT_MODE);
    }

    @Test
    void validateRequest_shouldThrowExceptionWhenCodeMissingInCodeRequiredModes() {
        AiLessonChatRequest request = new AiLessonChatRequest(
                null, 1L, "slug", "GEMINI", "DEBUG", "Help me", null, null, null, null,
                Collections.emptyList());

        assertThatThrownBy(() -> aiLessonChatService.chat(request, UUID.randomUUID()))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("error", ErrorCode.CODE_REQUIRED);
    }

    @Test
    void chat_shouldVerifyOwnershipAndThrowNotFoundWhenNotMatching() {
        UUID userId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();

        AiLessonChatRequest request = new AiLessonChatRequest(
                conversationId, 1L, "slug", "GEMINI", "EXPLAIN", "Explain loops", null, null, null,
                null, Collections.emptyList());

        // Ownership mismatch: findByIdAndUserIdAndType returns empty
        when(conversationRepository.findByIdAndUserIdAndType(conversationId, userId, ConversationType.LESSON))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> aiLessonChatService.chat(request, userId))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("error", ErrorCode.CONVERSATION_NOT_FOUND);
    }

    @Test
    void chat_shouldThrowNoMoreHintsWhenLimitExceeded() {
        UUID userId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        Long lessonId = 10L;

        AiLessonChatRequest request = new AiLessonChatRequest(
                conversationId, lessonId, "slug", "GEMINI", "HINT", "I need a hint", null, null, null,
                null, Collections.emptyList());

        AIConversation conversation = new AIConversation();
        conversation.setId(conversationId);
        conversation.setUserId(userId);
        conversation.setLessonId(lessonId);

        CodingLesson codingLesson = new CodingLesson();
        codingLesson.setId(lessonId);
        codingLesson.setHints(List.of("Hint 1", "Hint 2", "Hint 3")); // 3 hints available

        when(conversationRepository.findByIdAndUserIdAndType(conversationId, userId, ConversationType.LESSON))
                .thenReturn(Optional.of(conversation));
        when(conversationRepository.save(any(AIConversation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(codingLesson));

        // 3 hints already given in conversation history (N = 3, M = min(5, 3) = 3) ->
        // canAskNextHint is false
        when(aiMessageRepository.countByConversationIdAndRoleAndMode(conversationId, AiMessageRole.ASSISTANT,
                "HINT"))
                .thenReturn(3L);

        assertThatThrownBy(() -> aiLessonChatService.chat(request, userId))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("error", ErrorCode.NO_MORE_HINTS);
    }

    @Test
    void chat_shouldProcessSuccessfullyAndReturnQuickActionsAndCanAskNextHint() {
        UUID userId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        Long lessonId = 10L;

        AiLessonChatRequest request = new AiLessonChatRequest(
                conversationId, lessonId, "slug", "GEMINI", "HINT", "Give me first hint", null, null,
                null, null, Collections.emptyList());

        AIConversation conversation = new AIConversation();
        conversation.setId(conversationId);
        conversation.setUserId(userId);
        conversation.setLessonId(lessonId);

        CodingLesson codingLesson = new CodingLesson();
        codingLesson.setId(lessonId);
        codingLesson.setHints(List.of("Hint 1", "Hint 2", "Hint 3")); // 3 hints available

        when(conversationRepository.findByIdAndUserIdAndType(conversationId, userId, ConversationType.LESSON))
                .thenReturn(Optional.of(conversation));
        when(conversationRepository.save(any(AIConversation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(codingLesson));

        // 0 hints requested yet (N = 0, M = 3) -> canAskNextHintBefore is true
        when(aiMessageRepository.countByConversationIdAndRoleAndMode(conversationId, AiMessageRole.ASSISTANT,
                "HINT"))
                .thenReturn(0L);

        when(aiContextService.buildContext(any())).thenReturn("Context");
        when(aiPromptService.buildSystemPrompt(AiChatMode.HINT, false)).thenReturn("System Prompt");
        when(aiPromptService.buildUserPrompt(any(), any())).thenReturn("User Prompt");

        // Mock callLlmWithTokens to return AI answer
        doReturn(new AiLlmExecutor.ChatResponseWithTokens("Here is your hint.", null, null))
                .when(aiLessonChatService).callLlmWithTokens(any(), any(), any());
        when(aiResponseGuardrailService.enforceLessonDisclosurePolicy("Here is your hint.", AiChatMode.HINT, false))
                .thenReturn("Here is your hint.");

        when(suggestionGenerator.generate(eq(AiChatMode.HINT), any(), anyBoolean(), anyBoolean(), eq(true)))
                .thenReturn(List.of());

        AiChatResponse response = aiLessonChatService.chat(request, userId);

        assertThat(response.conversationId()).isEqualTo(conversationId);
        assertThat(response.answer()).isEqualTo("Here is your hint.");
        assertThat(response.canAskNextHint()).isTrue(); // N = 1 (after current), M = 3 -> 1 < 3 -> true
    }
}

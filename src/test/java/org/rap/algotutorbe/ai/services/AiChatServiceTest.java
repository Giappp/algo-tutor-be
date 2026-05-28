package org.rap.algotutorbe.ai.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rap.algotutorbe.ai.dto.AiChatRequest;
import org.rap.algotutorbe.ai.dto.AiChatResponse;
import org.rap.algotutorbe.ai.entity.AIConversation;
import org.rap.algotutorbe.ai.entity.AiMessage;
import org.rap.algotutorbe.ai.enums.AiChatMode;
import org.rap.algotutorbe.ai.enums.AiMessageRole;
import org.rap.algotutorbe.ai.repository.AiMessageRepository;
import org.rap.algotutorbe.ai.repository.ConversationRepository;
import org.rap.algotutorbe.ai.tools.AlgoTutorAiTools;
import org.rap.algotutorbe.common.errors.ErrorCode;
import org.rap.algotutorbe.common.exception.AppException;
import org.rap.algotutorbe.learning.models.CodingLesson;
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
class AiChatServiceTest {

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
        private org.rap.algotutorbe.learning.repositories.LearningPathRepository learningPathRepository;

        @InjectMocks
        @Spy
        private AiChatService aiChatService;

        @Test
        void validateRequest_shouldThrowExceptionWhenModeInvalid() {
                AiChatRequest request = new AiChatRequest(
                                null, 1L, null, "GEMINI", "INVALID_MODE", "message", null, null, null, null,
                                Collections.emptyList());

                assertThatThrownBy(() -> aiChatService.chat(request, UUID.randomUUID()))
                                .isInstanceOf(AppException.class)
                                .hasFieldOrPropertyWithValue("error", ErrorCode.INVALID_CHAT_MODE);
        }

        @Test
        void validateRequest_shouldThrowExceptionWhenCodeMissingInCodeRequiredModes() {
                AiChatRequest request = new AiChatRequest(
                                null, 1L, null, "GEMINI", "DEBUG", "Help me", null, null, null, null,
                                Collections.emptyList());

                assertThatThrownBy(() -> aiChatService.chat(request, UUID.randomUUID()))
                                .isInstanceOf(AppException.class)
                                .hasFieldOrPropertyWithValue("error", ErrorCode.CODE_REQUIRED);
        }

        @Test
        void chat_shouldVerifyOwnershipAndThrowNotFoundWhenNotMatching() {
                UUID userId = UUID.randomUUID();
                UUID conversationId = UUID.randomUUID();

                AiChatRequest request = new AiChatRequest(
                                conversationId, null, null, "GEMINI", "EXPLAIN", "Explain loops", null, null, null,
                                null, Collections.emptyList());

                // Ownership mismatch: findByIdAndUserId returns empty
                when(conversationRepository.findByIdAndUserId(conversationId, userId)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> aiChatService.chat(request, userId))
                                .isInstanceOf(AppException.class)
                                .hasFieldOrPropertyWithValue("error", ErrorCode.CONVERSATION_NOT_FOUND);
        }

        @Test
        void chat_shouldThrowNoMoreHintsWhenLimitExceeded() {
                UUID userId = UUID.randomUUID();
                UUID conversationId = UUID.randomUUID();
                Long lessonId = 10L;

                AiChatRequest request = new AiChatRequest(
                                conversationId, lessonId, null, "GEMINI", "HINT", "I need a hint", null, null, null,
                                null, Collections.emptyList());

                AIConversation conversation = new AIConversation();
                conversation.setId(conversationId);
                conversation.setUserId(userId);
                conversation.setLessonId(lessonId);

                CodingLesson codingLesson = new CodingLesson();
                codingLesson.setId(lessonId);
                codingLesson.setHints(List.of("Hint 1", "Hint 2", "Hint 3")); // 3 hints available

                when(conversationRepository.findByIdAndUserId(conversationId, userId))
                                .thenReturn(Optional.of(conversation));
                when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(codingLesson));

                // 3 hints already given in conversation history (N = 3, M = min(5, 3) = 3) ->
                // canAskNextHint is false
                when(aiMessageRepository.countByConversationIdAndRoleAndMode(conversationId, AiMessageRole.ASSISTANT,
                                "HINT"))
                                .thenReturn(3L);

                assertThatThrownBy(() -> aiChatService.chat(request, userId))
                                .isInstanceOf(AppException.class)
                                .hasFieldOrPropertyWithValue("error", ErrorCode.NO_MORE_HINTS);
        }

        @Test
        void chat_shouldProcessSuccessfullyAndReturnQuickActionsAndCanAskNextHint() {
                UUID userId = UUID.randomUUID();
                UUID conversationId = UUID.randomUUID();
                Long lessonId = 10L;

                AiChatRequest request = new AiChatRequest(
                                conversationId, lessonId, null, "GEMINI", "HINT", "Give me first hint", null, null,
                                null, null, Collections.emptyList());

                AIConversation conversation = new AIConversation();
                conversation.setId(conversationId);
                conversation.setUserId(userId);
                conversation.setLessonId(lessonId);

                CodingLesson codingLesson = new CodingLesson();
                codingLesson.setId(lessonId);
                codingLesson.setHints(List.of("Hint 1", "Hint 2", "Hint 3")); // 3 hints available

                when(conversationRepository.findByIdAndUserId(conversationId, userId))
                                .thenReturn(Optional.of(conversation));
                when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(codingLesson));

                // 0 hints requested yet (N = 0, M = 3) -> canAskNextHintBefore is true
                when(aiMessageRepository.countByConversationIdAndRoleAndMode(conversationId, AiMessageRole.ASSISTANT,
                                "HINT"))
                                .thenReturn(0L);

                when(aiContextService.buildContext(request)).thenReturn("Context");
                when(aiPromptService.buildSystemPrompt(AiChatMode.HINT)).thenReturn("System Prompt");
                when(aiPromptService.buildUserPrompt(any(), any(), any())).thenReturn("User Prompt");

                // Mock callLlmWithTokens to return AI answer
                doReturn(new AiChatService.ChatResponseWithTokens("Here is your hint.", null, null))
                                .when(aiChatService).callLlmWithTokens(any(), any());

                when(suggestionGenerator.generate(eq(AiChatMode.HINT), any(), anyBoolean(), anyBoolean(), eq(true)))
                                .thenReturn(List.of());

                AiChatResponse response = aiChatService.chat(request, userId);

                assertThat(response.conversationId()).isEqualTo(conversationId);
                assertThat(response.answer()).isEqualTo("Here is your hint.");
                assertThat(response.canAskNextHint()).isTrue(); // N = 1 (after current), M = 3 -> 1 < 3 -> true
        }
}

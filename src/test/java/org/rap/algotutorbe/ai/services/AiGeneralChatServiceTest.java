package org.rap.algotutorbe.ai.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rap.algotutorbe.ai.dto.AiGeneralChatRequest;
import org.rap.algotutorbe.ai.dto.AiGeneralChatResponse;
import org.rap.algotutorbe.ai.entity.AIConversation;
import org.rap.algotutorbe.ai.enums.AiGeneralChatIntent;
import org.rap.algotutorbe.ai.enums.ConversationType;
import org.rap.algotutorbe.ai.repository.AiMessageRepository;
import org.rap.algotutorbe.ai.repository.ConversationRepository;
import org.rap.algotutorbe.common.errors.ErrorCode;
import org.rap.algotutorbe.common.exception.AppException;
import org.rap.algotutorbe.learning.repositories.LearningPathRepository;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiGeneralChatServiceTest {

        @Mock
        private AiPromptService aiPromptService;
        @Mock
        private ConversationRepository conversationRepository;
        @Mock
        private ProviderRouter providerRouter;
        @Mock
        private AiMessageRepository aiMessageRepository;
        @Mock
        private AiMessagePersister aiMessagePersister;
        @Mock
        private LearningPathRepository learningPathRepository;
        @Mock
        private AiLlmExecutor aiLlmExecutor;
        @Mock
        private AiGeneralChatIntentClassifier intentClassifier;

        @InjectMocks
        @Spy
        private AiGeneralChatService aiGeneralChatService;

        @Test
        void validateRequest_shouldThrowExceptionWhenMessageBlank() {
                AiGeneralChatRequest request = new AiGeneralChatRequest(
                                null, "GEMINI", "   ");

                assertThatThrownBy(() -> aiGeneralChatService.generalChat(request, UUID.randomUUID(), Collections.emptyList()))
                                .isInstanceOf(AppException.class)
                                .hasFieldOrPropertyWithValue("error", ErrorCode.INVALID_PAYLOAD);
        }

        @Test
        void chat_shouldVerifyOwnershipAndThrowNotFoundWhenNotMatching() {
                UUID userId = UUID.randomUUID();
                UUID conversationId = UUID.randomUUID();

                AiGeneralChatRequest request = new AiGeneralChatRequest(
                                conversationId, "GEMINI", "Tư vấn lộ trình");

                // Ownership mismatch: findByIdAndUserIdAndType returns empty
                when(conversationRepository.findByIdAndUserIdAndType(conversationId, userId, ConversationType.GENERAL))
                                .thenReturn(Optional.empty());

                assertThatThrownBy(() -> aiGeneralChatService.generalChat(request, userId, Collections.emptyList()))
                                .isInstanceOf(AppException.class)
                                .hasFieldOrPropertyWithValue("error", ErrorCode.CONVERSATION_NOT_FOUND);
        }

        @Test
        void chat_shouldProcessSuccessfully() {
                UUID userId = UUID.randomUUID();
                UUID conversationId = UUID.randomUUID();

                AiGeneralChatRequest request = new AiGeneralChatRequest(
                                conversationId, "GEMINI", "Tư vấn lộ trình");

                AIConversation conversation = new AIConversation();
                conversation.setId(conversationId);
                conversation.setUserId(userId);

                when(conversationRepository.findByIdAndUserIdAndType(conversationId, userId, ConversationType.GENERAL))
                                .thenReturn(Optional.of(conversation));
                when(conversationRepository.save(any(AIConversation.class))).thenAnswer(invocation -> invocation.getArgument(0));
                when(intentClassifier.classify(request.message(), conversation))
                                .thenReturn(AiGeneralChatIntent.ROADMAP_ADVISORY);

                when(aiPromptService.buildGeneralSystemPrompt()).thenReturn("System Prompt");

                // Mock callLlmWithTokens to return AI answer
                doReturn(new AiLlmExecutor.ChatResponseWithTokens("Here is your roadmap.", null, null))
                                .when(aiGeneralChatService).callLlmWithTokens(any(), any(), any());

                AiGeneralChatResponse response = aiGeneralChatService.generalChat(request, userId, Collections.emptyList());

                assertThat(response.conversationId()).isEqualTo(conversationId);
                assertThat(response.answer()).isEqualTo("Here is your roadmap.");
        }
}

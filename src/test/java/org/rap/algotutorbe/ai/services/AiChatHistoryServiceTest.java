package org.rap.algotutorbe.ai.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rap.algotutorbe.ai.entity.AIConversation;
import org.rap.algotutorbe.ai.entity.AiMessage;
import org.rap.algotutorbe.ai.enums.AiMessageRole;
import org.rap.algotutorbe.ai.enums.ConversationType;
import org.rap.algotutorbe.ai.enums.LLMProvider;
import org.rap.algotutorbe.ai.repository.AiMessageRepository;
import org.rap.algotutorbe.ai.repository.ConversationRepository;
import org.rap.algotutorbe.common.errors.ErrorCode;
import org.rap.algotutorbe.common.exception.AppException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiChatHistoryServiceTest {

    @Mock
    private ConversationRepository conversationRepository;
    @Mock
    private AiMessageRepository aiMessageRepository;
    @InjectMocks
    private AiChatHistoryService aiChatHistoryService;

    @Test
    void getLessonChatHistory_shouldReturnMessagesInRepositoryOrder() {
        UUID userId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        AIConversation conversation = conversation(conversationId, userId, ConversationType.LESSON);
        AiMessage userMessage = message(conversationId, userId, AiMessageRole.USER, "Help me", "HINT");
        AiMessage assistantMessage = message(conversationId, userId, AiMessageRole.ASSISTANT, "Try a hash map", "HINT");

        when(conversationRepository.findByIdAndUserIdAndType(conversationId, userId, ConversationType.LESSON))
                .thenReturn(Optional.of(conversation));
        when(aiMessageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId))
                .thenReturn(List.of(userMessage, assistantMessage));

        var response = aiChatHistoryService.getLessonChatHistory(conversationId, userId);

        assertThat(response.conversationId()).isEqualTo(conversationId);
        assertThat(response.type()).isEqualTo(ConversationType.LESSON);
        assertThat(response.messages()).extracting(message -> message.role())
                .containsExactly(AiMessageRole.USER, AiMessageRole.ASSISTANT);
        assertThat(response.messages()).extracting(message -> message.content())
                .containsExactly("Help me", "Try a hash map");
        assertThat(response.messages()).extracting(message -> message.mode())
                .containsExactly("HINT", "HINT");
    }

    @Test
    void getGeneralChatHistory_shouldVerifyOwnershipAndType() {
        UUID userId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        when(conversationRepository.findByIdAndUserIdAndType(conversationId, userId, ConversationType.GENERAL))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> aiChatHistoryService.getGeneralChatHistory(conversationId, userId))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("error", ErrorCode.CONVERSATION_NOT_FOUND);

        verify(conversationRepository)
                .findByIdAndUserIdAndType(conversationId, userId, ConversationType.GENERAL);
    }

    private AIConversation conversation(UUID conversationId, UUID userId, ConversationType type) {
        AIConversation conversation = new AIConversation();
        conversation.setId(conversationId);
        conversation.setUserId(userId);
        conversation.setLessonId(type == ConversationType.LESSON ? 1L : null);
        conversation.setTitle("Conversation title");
        conversation.setProvider(LLMProvider.GEMINI);
        conversation.setType(type);
        conversation.setCreatedAt(Instant.parse("2026-06-10T00:00:00Z"));
        conversation.setUpdatedAt(Instant.parse("2026-06-10T00:01:00Z"));
        return conversation;
    }

    private AiMessage message(
            UUID conversationId,
            UUID userId,
            AiMessageRole role,
            String content,
            String mode) {
        AiMessage message = new AiMessage();
        message.setId(UUID.randomUUID());
        message.setConversationId(conversationId);
        message.setUserId(userId);
        message.setRole(role);
        message.setContent(content);
        message.setMode(mode);
        message.setCreatedAt(Instant.now());
        return message;
    }
}

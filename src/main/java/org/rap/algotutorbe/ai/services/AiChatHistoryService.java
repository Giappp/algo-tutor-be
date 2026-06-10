package org.rap.algotutorbe.ai.services;

import lombok.RequiredArgsConstructor;
import org.rap.algotutorbe.ai.dto.AiChatHistoryMessageResponse;
import org.rap.algotutorbe.ai.dto.AiChatHistoryResponse;
import org.rap.algotutorbe.ai.entity.AIConversation;
import org.rap.algotutorbe.ai.entity.AiMessage;
import org.rap.algotutorbe.ai.enums.ConversationType;
import org.rap.algotutorbe.ai.repository.AiMessageRepository;
import org.rap.algotutorbe.ai.repository.ConversationRepository;
import org.rap.algotutorbe.common.errors.ErrorCode;
import org.rap.algotutorbe.common.exception.AppException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AiChatHistoryService {

    private final ConversationRepository conversationRepository;
    private final AiMessageRepository aiMessageRepository;

    @Transactional(readOnly = true)
    public AiChatHistoryResponse getLessonChatHistory(UUID conversationId, UUID userId) {
        return getHistory(conversationId, userId, ConversationType.LESSON);
    }

    @Transactional(readOnly = true)
    public AiChatHistoryResponse getGeneralChatHistory(UUID conversationId, UUID userId) {
        return getHistory(conversationId, userId, ConversationType.GENERAL);
    }

    private AiChatHistoryResponse getHistory(UUID conversationId, UUID userId, ConversationType type) {
        AIConversation conversation = conversationRepository
                .findByIdAndUserIdAndType(conversationId, userId, type)
                .orElseThrow(() -> new AppException(ErrorCode.CONVERSATION_NOT_FOUND));

        var messages = aiMessageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId)
                .stream()
                .map(this::toMessageResponse)
                .toList();

        return new AiChatHistoryResponse(
                conversation.getId(),
                conversation.getType(),
                conversation.getLessonId(),
                conversation.getTitle(),
                conversation.getProvider(),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt(),
                messages);
    }

    private AiChatHistoryMessageResponse toMessageResponse(AiMessage message) {
        return new AiChatHistoryMessageResponse(
                message.getId(),
                message.getRole(),
                message.getContent(),
                message.getMode(),
                message.getCreatedAt());
    }
}

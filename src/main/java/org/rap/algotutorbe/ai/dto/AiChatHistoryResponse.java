package org.rap.algotutorbe.ai.dto;

import org.rap.algotutorbe.ai.enums.ConversationType;
import org.rap.algotutorbe.ai.enums.LLMProvider;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AiChatHistoryResponse(
        UUID conversationId,
        ConversationType type,
        Long lessonId,
        String title,
        LLMProvider provider,
        Instant createdAt,
        Instant updatedAt,
        List<AiChatHistoryMessageResponse> messages
) {
}

package org.rap.algotutorbe.ai.dto;

import org.rap.algotutorbe.ai.enums.AiMessageRole;

import java.time.Instant;
import java.util.UUID;

public record AiChatHistoryMessageResponse(
        UUID id,
        AiMessageRole role,
        String content,
        String mode,
        Instant createdAt
) {
}

package org.rap.algotutorbe.ai.dto;

import java.util.List;
import java.util.UUID;

public record AiChatResponse(
        UUID conversationId,
        String answer,
        String mode,
        List<AiQuickAction> quickActions,
        List<AiSource> sources,
        Boolean canAskNextHint
) {
}
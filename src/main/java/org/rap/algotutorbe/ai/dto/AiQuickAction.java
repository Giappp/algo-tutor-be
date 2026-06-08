package org.rap.algotutorbe.ai.dto;

import org.rap.algotutorbe.ai.enums.AiChatIntent;

public record AiQuickAction(
        String label,
        AiChatIntent intent,
        String mode,
        String message
) {
}

package org.rap.algotutorbe.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record AiGeneralChatRequest(
        UUID conversationId,
        String provider,
        @NotBlank(message = "Message must not be blank")
        @Size(max = 5000, message = "Message must not exceed 5000 characters")
        String message
) {
}

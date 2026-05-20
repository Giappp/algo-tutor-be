package org.rap.algotutorbe.ai.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.rap.algotutorbe.ai.dto.validation.AtLeastOneNotBlank;

import java.util.List;
import java.util.UUID;

@AtLeastOneNotBlank(
        fields = {"message", "code"},
        message = "At least one of message or code must be provided"
)
public record AiChatRequest(
        UUID conversationId,
        Long lessonId,
        String lessonSlug,
        String provider,
        @NotNull(message = "Mode must not be null")
        String mode,
        @Size(max = 5000, message = "Message must not exceed 5000 characters")
        String message,
        @Size(max = 10000, message = "Code must not exceed 10000 characters")
        String code,
        String language,
        String judgeResult,
        String errorMessage,
        List<String> failedTestCases
) {
}

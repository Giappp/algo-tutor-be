package org.rap.algotutorbe.ai.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AiSuggestion(
        @NotNull
        @Size(max = 100, message = "Label must not exceed 100 characters")
        String label,
        @NotNull
        String mode,
        @NotNull
        @Size(max = 500, message = "Message must not exceed 500 characters")
        String message
) {
}

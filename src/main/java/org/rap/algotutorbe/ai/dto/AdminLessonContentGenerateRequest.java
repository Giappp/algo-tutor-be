package org.rap.algotutorbe.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminLessonContentGenerateRequest(
        String provider,
        @NotBlank(message = "Prompt must not be blank")
        @Size(max = 5000, message = "Prompt must not exceed 5000 characters")
        String prompt
) {
}

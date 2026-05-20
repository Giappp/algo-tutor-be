package org.rap.algotutorbe.judge.dto;

import jakarta.validation.constraints.NotBlank;

public record JudgeRequest(
        @NotBlank String lessonSlug,
        @NotBlank String language,
        @NotBlank String code
) {
}

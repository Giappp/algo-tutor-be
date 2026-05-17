package org.rap.algotutorbe.judge.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for POST /judge/run and POST /judge/submit.
 * Frontend sends lessonSlug, language, and code.
 */
public record JudgeRunRequest(
        @NotBlank String lessonSlug,
        @NotBlank String language,
        @NotBlank String code
) {
}

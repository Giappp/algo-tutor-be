package org.rap.algotutorbe.submission.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SubmitCodeRequest(
        @NotNull Long problemId,
        @NotNull Long userId,
        @NotBlank String sourceCode,
        @NotNull String language
) {
}

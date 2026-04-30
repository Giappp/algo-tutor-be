package org.rap.algotutorbe.learning.dto;

import jakarta.validation.constraints.NotBlank;

public record TestCaseDTO(
        @NotBlank String stdin,
        @NotBlank String expectedStdout,
        Boolean isHidden,
        Integer orderIndex,
        String explanation
) {
}

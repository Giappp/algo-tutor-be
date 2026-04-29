package org.rap.algotutorbe.learning.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.rap.algotutorbe.learning.enums.ProgrammingLanguage;

public record EditorialRequestDTO(
        @NotNull ProgrammingLanguage language,
        @NotBlank String sourceCode
) {
}

package org.rap.algotutorbe.problem.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.rap.algotutorbe.problem.domain.enums.ProgrammingLanguage;

public record CreateEditorialRequest(
        @NotNull(message = "language.required")
        ProgrammingLanguage language,

        @NotBlank(message = "sourceCode.required")
        String sourceCode
) {
}

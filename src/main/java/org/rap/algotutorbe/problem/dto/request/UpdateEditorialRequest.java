package org.rap.algotutorbe.problem.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateEditorialRequest(
        @NotBlank(message = "sourceCode.required")
        String sourceCode
) {
}

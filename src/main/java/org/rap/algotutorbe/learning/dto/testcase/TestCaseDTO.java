package org.rap.algotutorbe.learning.dto.testcase;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TestCaseDTO(
        Long id,
        @NotBlank String inputFileUrl,
        @NotBlank String outputFileUrl,
        @NotBlank String inputFileKey,
        @NotBlank String outputFileKey,
        @NotNull Integer scoreWeight,
        Boolean isSample,
        Integer sortOrder
) {
}

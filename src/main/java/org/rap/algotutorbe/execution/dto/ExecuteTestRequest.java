package org.rap.algotutorbe.execution.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record ExecuteTestRequest(
        @NotBlank String problemSlug,
        @NotBlank String language,
        @NotBlank String code,
        List<ExecuteTestcase> testCases
) {
}


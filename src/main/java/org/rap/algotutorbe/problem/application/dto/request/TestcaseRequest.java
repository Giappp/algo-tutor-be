package org.rap.algotutorbe.problem.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TestcaseRequest(Long id,
                              @NotNull @NotBlank String input,
                              @NotNull @NotBlank String expectedOutput,
                              Boolean isSample,
                              Integer orderIndex,
                              String explanation
) {
}

package org.rap.algotutorbe.problem.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TestcaseRequest(Long id,
                              @NotNull @NotBlank String stdin,
                              @NotNull @NotBlank String expectedStdout,
                              Boolean isSample,
                              Integer orderIndex,
                              String explanation
) {
}

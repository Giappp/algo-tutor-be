package org.rap.algotutorbe.problem.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TestcaseDto(@NotNull @NotBlank String input,
                          @NotNull @NotBlank String expectedOutput,
                          Integer orderIndex) {
}

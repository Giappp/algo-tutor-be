package org.rap.algotutorbe.problem.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.rap.algotutorbe.problem.domain.enums.ProgrammingLanguage;

public record ModelSolutionRequest(@NotBlank String code,
                                   @NotNull ProgrammingLanguage language) {
}

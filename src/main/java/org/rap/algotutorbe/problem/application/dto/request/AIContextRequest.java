package org.rap.algotutorbe.problem.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AIContextRequest(@NotNull @NotBlank String algorithmicConcept,
                               @NotNull @NotBlank String predefinedHints,
                               @NotNull @NotBlank String edgeCasesToRemind) {
}

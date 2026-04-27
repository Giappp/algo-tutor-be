package org.rap.algotutorbe.problem.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AIContextRequest(@NotNull @NotBlank String algorithmicConcept,
                               @NotNull @NotBlank String predefinedHints,
                               @NotNull @NotBlank String edgeCasesToRemind) {
}

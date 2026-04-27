package org.rap.algotutorbe.problem.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateOrCreateTagRequest(@NotNull @NotBlank String name) {
}

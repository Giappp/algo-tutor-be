package org.rap.algotutorbe.problem.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateTagRequest(@NotNull @NotBlank String name) {
}

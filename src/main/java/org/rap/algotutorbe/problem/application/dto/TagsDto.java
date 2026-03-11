package org.rap.algotutorbe.problem.application.dto;

import jakarta.validation.constraints.NotNull;

public record TagsDto(@NotNull Long id, @NotNull String name) {
}

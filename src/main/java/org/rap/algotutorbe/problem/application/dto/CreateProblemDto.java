package org.rap.algotutorbe.problem.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record CreateProblemDto(@NotNull String slug,
                               @NotNull String title,
                               @NotNull @NotBlank String statement,
                               @NotNull String difficulty,
                               Set<TagsDto> tags) {
}

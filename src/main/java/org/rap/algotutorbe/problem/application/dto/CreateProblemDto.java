package org.rap.algotutorbe.problem.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateProblemDto(@NotNull String slug,
                               @NotNull String title,
                               @NotNull @NotBlank String statement,
                               @NotNull String difficulty,
                               @NotNull String status,
                               List<TagsDto> tags) {
}

package org.rap.algotutorbe.problem.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record CreateProblemDto(@NotNull @NotBlank(message = "slug.required") String slug,
                               @NotNull @NotBlank(message = "title.required") String title,
                               @NotNull @NotBlank(message = "statement.required") String statement,
                               @NotNull @NotBlank(message = "difficulty.required") String difficulty,
                               Set<TagsDto> tags) {
}

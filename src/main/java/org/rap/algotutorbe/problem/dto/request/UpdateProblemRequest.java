package org.rap.algotutorbe.problem.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.rap.algotutorbe.problem.dto.TagDto;

import java.util.Set;

public record UpdateProblemRequest(
        @NotNull @NotBlank(message = "title.required") String title,
        @NotNull @NotBlank(message = "statement.required") String statement,
        @NotNull @NotBlank(message = "difficulty.required") String difficulty,
        Set<TagDto> tags
) {
}

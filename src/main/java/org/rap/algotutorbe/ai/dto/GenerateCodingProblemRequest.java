package org.rap.algotutorbe.ai.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.rap.algotutorbe.ai.enums.LLMProvider;
import org.rap.algotutorbe.learning.enums.Difficulty;

import java.util.List;

public record GenerateCodingProblemRequest(
        @NotNull @Size(max = 10) List<@NotNull Long> sourceLessonIds,
        LLMProvider provider,
        @Size(max = 3000) String prompt,
        @NotNull Difficulty difficulty,
        @NotNull @Min(1) @Max(4) Integer exampleCount,
        @NotNull @Min(0) @Max(3) Integer hintCount
) {
}

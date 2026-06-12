package org.rap.algotutorbe.ai.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.rap.algotutorbe.ai.enums.LLMProvider;
import org.rap.algotutorbe.learning.enums.Difficulty;
import org.rap.algotutorbe.learning.models.QuestionType;

import java.util.List;
import java.util.Set;

public record GenerateQuestionsFromSourcesRequest(
        @NotEmpty
        @Size(max = 10)
        List<@NotNull Long> sourceLessonIds,
        @Size(max = 2000)
        String prompt,
        LLMProvider provider,
        @NotNull
        Difficulty difficulty,
        @NotEmpty
        @Size(max = 2)
        Set<@NotNull QuestionType> questionTypes,
        @NotNull
        @Min(1)
        @Max(10)
        Integer count,
        @NotNull
        @Min(2)
        @Max(5)
        Integer choicesPerQuestion,
        @NotNull
        Boolean includeExplanations
) {
}

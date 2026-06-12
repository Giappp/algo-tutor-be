package org.rap.algotutorbe.ai.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.rap.algotutorbe.ai.enums.LLMProvider;
import org.rap.algotutorbe.learning.enums.ProgrammingLanguage;

import java.util.List;

public record GenerateCodingEditorialRequest(
        @NotNull @Size(max = 10) List<@NotNull Long> sourceLessonIds,
        LLMProvider provider,
        @Size(max = 3000) String prompt,
        @NotNull ProgrammingLanguage language
) {
}

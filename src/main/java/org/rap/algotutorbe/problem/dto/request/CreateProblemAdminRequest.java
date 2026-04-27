package org.rap.algotutorbe.problem.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.rap.algotutorbe.problem.domain.enums.Difficulty;
import org.rap.algotutorbe.problem.domain.models.ProblemExample;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record CreateProblemAdminRequest(
        @NotBlank(message = "title.required")
        @Size(max = 255, message = "title.max")
        String title,

        @NotBlank(message = "statement.required")
        String statement,

        @NotNull(message = "difficulty.required")
        Difficulty difficulty,

        @NotNull(message = "examples.required")
        List<ProblemExample> examples,

        @NotNull(message = "constraints.required")
        List<String> constraints,

        List<String> hints,

        List<String> keyInsights,

        @NotNull(message = "starterCode.required")
        Map<String, String> starterCode,

        Integer baseTimeLimitMs,

        Integer baseMemoryLimitMb,

        Set<Long> tags
) {
    public CreateProblemAdminRequest {
        if (baseTimeLimitMs == null) baseTimeLimitMs = 1000;
        if (baseMemoryLimitMb == null) baseMemoryLimitMb = 256;
    }
}

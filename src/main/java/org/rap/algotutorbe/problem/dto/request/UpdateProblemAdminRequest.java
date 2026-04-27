package org.rap.algotutorbe.problem.dto.request;

import jakarta.validation.constraints.Size;
import org.rap.algotutorbe.problem.domain.enums.Difficulty;
import org.rap.algotutorbe.problem.domain.models.ProblemExample;

import java.util.List;
import java.util.Map;

public record UpdateProblemAdminRequest(
        @Size(max = 255, message = "title.max")
        String title,

        String statement,

        Difficulty difficulty,

        List<ProblemExample> examples,

        List<String> constraints,

        Map<String, String> starterCode,

        List<String> hints,

        List<String> keyInsights,

        Integer baseTimeLimitMs,

        Integer baseMemoryLimitMb,

        List<String> tags
) {
}

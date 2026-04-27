package org.rap.algotutorbe.problem.dto.response;

import org.rap.algotutorbe.problem.domain.enums.Difficulty;
import org.rap.algotutorbe.problem.domain.models.ProblemExample;
import org.rap.algotutorbe.problem.dto.TagDto;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record ProblemDetailAdminResponse(
        Long id,
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
        Set<TagDto> tags,
        Set<EditorialResponse> editorials,
        Instant createdAt,
        Instant updatedAt
) {
}

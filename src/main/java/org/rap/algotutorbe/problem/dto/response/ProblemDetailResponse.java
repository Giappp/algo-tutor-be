package org.rap.algotutorbe.problem.dto.response;

import org.rap.algotutorbe.problem.domain.models.ProblemExample;

import java.util.List;
import java.util.Map;

public record ProblemDetailResponse(
        Long id,
        String title,
        String difficulty,
        List<String> tags,
        String description,
        List<ProblemExample> examples,
        List<String> constraints,
        Map<String, String> starterCode,
        List<String> hints,
        List<String> keyInsights,
        List<String> relatedSlugs,
        String createdAt,
        String updatedAt
) {
}

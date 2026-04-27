package org.rap.algotutorbe.problem.dto.response;

import java.util.List;

public record ProblemSummaryResponse(
        Long id,
        String title,
        String difficulty,
        List<String> tags,
        String status,
        int examplesCount,
        int hintsCount
) {
}
package org.rap.algotutorbe.problem.application.dto.response;

import java.time.LocalDateTime;
import java.util.Set;

public record ProblemSummaryAdminResponse(
        Long id,
        String slug,
        String title,
        String difficulty,
        String status,
        boolean isBenchmarked,
        Set<String> tags,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}


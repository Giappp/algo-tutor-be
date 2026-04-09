package org.rap.algotutorbe.problem.application.dto.response;

import java.time.Instant;
import java.util.Set;

public record ProblemSummaryAdminResponse(
        Long id,
        String slug,
        String title,
        String difficulty,
        String status,
        Set<String> tags,
        Instant createdAt,
        Instant updatedAt
) {
}


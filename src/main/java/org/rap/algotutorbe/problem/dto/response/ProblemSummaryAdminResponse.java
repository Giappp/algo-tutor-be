package org.rap.algotutorbe.problem.dto.response;

import java.time.Instant;
import java.util.Set;

public record ProblemSummaryAdminResponse(
        Long id,
        String title,
        String difficulty,
        Set<String> tags,
        Instant createdAt,
        Instant updatedAt
) {
}


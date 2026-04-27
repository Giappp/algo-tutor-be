package org.rap.algotutorbe.problem.dto.response;

public record ProblemStatsResponse(
        long total,
        long solved,
        long attempted,
        long notStarted,
        long easy,
        long medium,
        long hard
) {
}


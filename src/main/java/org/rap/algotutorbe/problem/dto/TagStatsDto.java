package org.rap.algotutorbe.problem.dto;

import com.fasterxml.jackson.annotation.JsonInclude;


@JsonInclude(JsonInclude.Include.NON_NULL)
public record TagStatsDto(
        String name,
        String slug,
        Long totalProblems,
        DifficultyBreakdownDto difficultyBreakdown,
        java.math.BigDecimal avgAcceptance,
        UserProgressDto userProgress
) {
    public record DifficultyBreakdownDto(long easy, long medium, long hard) {
    }

    public record UserProgressDto(long solved, long attempted, long notStarted) {
    }
}

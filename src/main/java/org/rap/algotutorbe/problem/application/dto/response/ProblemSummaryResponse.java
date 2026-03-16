package org.rap.algotutorbe.problem.application.dto.response;

import org.rap.algotutorbe.problem.domain.enums.Difficulty;

import java.util.Set;

public record ProblemSummaryResponse(
        String slug,
        String title,
        Difficulty difficulty,
        Set<String> tags
) {
}
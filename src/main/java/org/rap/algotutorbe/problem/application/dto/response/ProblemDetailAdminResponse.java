package org.rap.algotutorbe.problem.application.dto.response;

import org.rap.algotutorbe.problem.domain.enums.ProgrammingLanguage;

import java.time.Instant;
import java.util.List;
import java.util.Set;

public record ProblemDetailAdminResponse(
        Long id,
        String slug,
        String title,
        String statement,
        String difficulty,
        String status,
        boolean isBenchmarked,
        String modelSolutionCode,
        ProgrammingLanguage modelSolutionLanguage,
        Set<String> tags,
        List<TestcaseAdminResponse> testcases,
        AIContextResponse aiContext,
        Long authorId,
        Instant createdAt,
        Instant updatedAt
) {
}

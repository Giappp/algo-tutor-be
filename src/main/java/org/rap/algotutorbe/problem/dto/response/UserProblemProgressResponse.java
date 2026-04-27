package org.rap.algotutorbe.problem.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserProblemProgressResponse(
        String problemSlug,
        String status,
        SubmissionSummaryResponse bestSubmission,
        Integer attempts,
        Integer timeSpentSeconds,
        String notes,
        Boolean bookmarked,
        Instant solvedAt
) {
}

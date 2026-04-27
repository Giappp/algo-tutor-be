package org.rap.algotutorbe.problem.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SubmissionSummaryResponse(
        UUID id,
        String language,
        Integer executionTime,
        Integer memoryUsed,
        Instant submittedAt
) {
}

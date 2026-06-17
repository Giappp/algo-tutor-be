package org.rap.algotutorbe.submission.dto;

import java.time.Instant;

public record SubmissionResponse(
        String id,
        String language,
        String status,
        Integer passedTestcases,
        Integer totalTestcases,
        Integer executionTime,
        Integer memoryUsed,
        Boolean progressUpdated,
        Instant submittedAt
) {
}

package org.rap.algotutorbe.submission.dto;

import java.time.Instant;

public record SubmissionResponse(
        String id,
        String language,
        String status,
        Integer passedTestCases,
        Integer totalTestCases,
        Integer executionTime,
        Integer memoryUsed,
        Instant submittedAt
) {
}
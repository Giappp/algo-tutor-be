package org.rap.algotutorbe.submission.dto;

import java.time.Instant;

public record SubmissionDetailResponse(
        String id,
        String language,
        String status,
        String sourceCode,
        Integer passedTestCases,
        Integer totalTestCases,
        Integer executionTime,
        Integer memoryUsed,
        String compileOutput,
        Instant submittedAt
) {
}

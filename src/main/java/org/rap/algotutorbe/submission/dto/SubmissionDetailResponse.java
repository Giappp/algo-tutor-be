package org.rap.algotutorbe.submission.dto;

import java.time.Instant;
import java.util.List;

public record SubmissionDetailResponse(
        String id,
        String language,
        String status,
        Integer passedTestCases,
        Integer totalTestCases,
        Integer executionTime,
        Integer memoryUsed,
        String compileOutput,
        List<SubmissionTestcaseResultResponse> results,
        Instant submittedAt
) {
}

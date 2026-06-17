package org.rap.algotutorbe.submission.dto;

import java.time.Instant;
import java.util.List;

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
        Boolean progressUpdated,
        Instant submittedAt,
        List<TestCaseResult> testCases
) {
    public SubmissionDetailResponse(
            String id,
            String language,
            String status,
            String sourceCode,
            Integer passedTestCases,
            Integer totalTestCases,
            Integer executionTime,
            Integer memoryUsed,
            String compileOutput,
            Boolean progressUpdated,
            Instant submittedAt
    ) {
        this(id, language, status, sourceCode, passedTestCases, totalTestCases, executionTime,
                memoryUsed, compileOutput, progressUpdated, submittedAt, List.of());
    }

    public record TestCaseResult(
            Integer index,
            String status,
            Integer timeMs,
            Integer memoryKb,
            String stdout,
            String stderr
    ) {
    }
}

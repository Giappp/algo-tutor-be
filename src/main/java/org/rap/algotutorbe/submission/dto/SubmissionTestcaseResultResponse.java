package org.rap.algotutorbe.submission.dto;

public record SubmissionTestcaseResultResponse(
        int testCase,
        String status,
        String input,
        String expected,
        String actual,
        Integer executionTime,
        String error
) {
}


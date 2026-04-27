package org.rap.algotutorbe.execution.dto;

public record ExecuteTestcaseResult(
        String testCaseId,
        String status,
        String input,
        String expected,
        String actual,
        Integer executionTime
) {
}


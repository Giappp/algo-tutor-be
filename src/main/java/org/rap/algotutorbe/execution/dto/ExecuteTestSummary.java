package org.rap.algotutorbe.execution.dto;

public record ExecuteTestSummary(
        int passed,
        int failed,
        int total
) {
}


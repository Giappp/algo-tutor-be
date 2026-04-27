package org.rap.algotutorbe.execution.dto;

import java.util.List;

public record ExecuteTestResponse(
        List<ExecuteTestcaseResult> results,
        ExecuteTestSummary summary
) {
}


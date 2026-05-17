package org.rap.algotutorbe.judge.dto;

import java.util.List;

/**
 * Response DTO for POST /judge/run.
 * Returns results for visible test cases only.
 */
public record JudgeRunResponse(
        List<TestCaseRunResult> results,
        Integer totalTime,
        String compilationError
) {
    public record TestCaseRunResult(
            String stdin,
            String expected,
            String actual,
            Boolean passed,
            Boolean hidden,
            Integer executionTime
    ) {
    }
}

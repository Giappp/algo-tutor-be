package org.rap.algotutorbe.judge.dto;

import java.util.List;

/**
 * Response DTO for POST /judge/submit.
 * Returns results for ALL test cases + auto-progress update info.
 */
public record JudgeSubmitResponse(
        String id,
        String status,
        List<TestCaseSubmitResult> results,
        Integer totalTime,
        Integer memoryUsed,
        String compilationError,
        Boolean lessonProgressUpdated
) {
    public record TestCaseSubmitResult(
            String stdin,
            String expected,
            String actual,
            Boolean passed,
            Boolean hidden,
            Integer executionTime
    ) {
    }
}

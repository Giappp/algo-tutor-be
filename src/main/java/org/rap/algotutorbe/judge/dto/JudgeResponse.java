package org.rap.algotutorbe.judge.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Unified response DTO for all judge endpoints: /judge/run and /judge/submit.
 * FE receives the same structure regardless of the operation type.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record JudgeResponse(
        String submissionId,
        String verdict,
        Summary summary,
        Performance performance,
        List<TestCaseResult> testCases,
        String compilationError,
        Boolean lessonProgressUpdated
) {

    public record Summary(
            int passed,
            int failed,
            int total
    ) {
    }

    public record Performance(
            Integer totalTimeMs,
            Integer maxMemoryKb
    ) {
    }

    public record TestCaseResult(
            int index,
            String status,
            String stdin,
            String expectedOutput,
            String actualOutput,
            Integer timeMs,
            Integer memoryKb,
            Boolean hidden,
            String errorMessage
    ) {
    }
}

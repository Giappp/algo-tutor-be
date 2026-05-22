package org.rap.algotutorbe.judge.dto;

import java.util.List;

public record JudgeResponse(
        String submissionId,
        String verdict,
        Summary summary,
        Performance performance,
        List<TestCaseResult> testCases,
        String compilationError,
        boolean progressUpdated
) {
    public record Summary(int passed, int total, int failed) {
    }

    public record Performance(Integer totalTimeMs, Integer maxMemoryKb) {
    }

    public record TestCaseResult(int index, String status, Integer timeMs, Integer memoryKb, String actualOutput) {
    }
}

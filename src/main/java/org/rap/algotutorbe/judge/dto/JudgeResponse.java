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
    public record Summary(int passed, int total, int failed, int executed) {
    }

    public record Performance(Integer maxTimeMs, Integer maxMemoryKb) {
    }

    public record TestCaseResult(
            int index,
            String status,
            Integer timeMs,
            Integer memoryKb,
            String stdout,
            String stderr
    ) {
    }
}

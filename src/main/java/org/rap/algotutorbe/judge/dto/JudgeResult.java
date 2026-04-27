package org.rap.algotutorbe.judge.dto;

import org.rap.algotutorbe.submission.entities.Verdict;

import java.util.List;

public record JudgeResult(
        Verdict verdict,
        int passedCount,
        int totalCount,
        double maxTime,
        long maxMemory,
        String compileOutput,
        List<TestcaseJudgeResult> details
) {
}

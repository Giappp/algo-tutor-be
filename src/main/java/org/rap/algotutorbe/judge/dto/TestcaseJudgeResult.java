package org.rap.algotutorbe.judge.dto;

import org.rap.algotutorbe.submission.entities.Verdict;

public record TestcaseJudgeResult(
        int index,
        Verdict verdict,
        Integer cpuTime,
        Integer memory,
        String stdout,
        String compileOutput
) {
}

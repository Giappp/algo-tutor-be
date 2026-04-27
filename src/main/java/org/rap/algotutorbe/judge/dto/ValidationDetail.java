package org.rap.algotutorbe.judge.dto;

import org.rap.algotutorbe.submission.entities.Verdict;

public record ValidationDetail(
        int testcaseIndex,
        Verdict verdict,
        String stdin,
        String expectedStdout,
        String actualOutput,
        String errorMessage
) {
}

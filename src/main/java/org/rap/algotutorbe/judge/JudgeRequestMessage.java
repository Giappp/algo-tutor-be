package org.rap.algotutorbe.judge;

public record JudgeRequestMessage(
        Long submissionId,
        Long problemId,
        String sourceCode,
        String language
) {
}

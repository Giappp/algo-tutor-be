package org.rap.algotutorbe.submission;

public record SubmissionCreatedMessage(
        Long submissionId,
        Long problemId,
        String sourceCode,
        String language     // tên enum
) {
}

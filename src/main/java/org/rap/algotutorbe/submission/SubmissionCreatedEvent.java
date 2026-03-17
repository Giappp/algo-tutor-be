package org.rap.algotutorbe.submission;

public record SubmissionCreatedEvent(
        Long submissionId,
        Long problemId,
        String sourceCode,
        String language
) {
}

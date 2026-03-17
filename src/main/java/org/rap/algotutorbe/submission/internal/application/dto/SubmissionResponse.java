package org.rap.algotutorbe.submission.internal.application.dto;

public record SubmissionResponse(
        Long submissionId,
        String status,
        String language,
        String submittedAt
) {
}
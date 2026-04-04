package org.rap.algotutorbe.submission.application.dto;

public record SubmissionResponse(
        Long submissionId,
        String verdict,
        String message
) {
}
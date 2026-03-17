package org.rap.algotutorbe.submission.internal.application.dto;

public record SubmitCodeRequest(
        Long problemId,
        String sourceCode,
        String language
) {
}

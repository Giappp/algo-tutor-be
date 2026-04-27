package org.rap.algotutorbe.submission.dto;

import org.rap.algotutorbe.problem.domain.enums.ProgrammingLanguage;

import java.util.UUID;

public record SubmissionCreatedMessage(
        UUID submissionId,
        Long problemId,
        String sourceCode,
        ProgrammingLanguage programmingLanguage
) {
}

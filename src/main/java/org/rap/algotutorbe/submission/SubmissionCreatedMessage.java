package org.rap.algotutorbe.submission;

import org.rap.algotutorbe.problem.domain.enums.ProgrammingLanguage;

public record SubmissionCreatedMessage(
        Long submissionId,
        Long problemId,
        String sourceCode,
        ProgrammingLanguage programmingLanguage
) {
}

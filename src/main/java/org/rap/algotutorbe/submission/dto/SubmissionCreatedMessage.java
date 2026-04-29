package org.rap.algotutorbe.submission.dto;

import org.rap.algotutorbe.learning.enums.ProgrammingLanguage;

import java.util.UUID;

public record SubmissionCreatedMessage(
        UUID submissionId,
        Long codingLessonId,
        String sourceCode,
        ProgrammingLanguage programmingLanguage
) {
}

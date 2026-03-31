package org.rap.algotutorbe.judge.application.dto;

import org.rap.algotutorbe.submission.domain.model.SubmissionStatus;

public record JudgeResult(
        SubmissionStatus status,
        int passedCount,
        int totalCount
) {
}

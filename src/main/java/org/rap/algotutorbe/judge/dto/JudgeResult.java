package org.rap.algotutorbe.judge.dto;

import org.rap.algotutorbe.submission.domain.model.SubmissionStatus;

import java.util.List;

public record JudgeResult(
        SubmissionStatus status,
        int passedCount,
        int totalCount,
        double time,
        long memory,
        List<TestcaseResultDto> details
) {
}

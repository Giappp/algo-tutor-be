package org.rap.algotutorbe.judge;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rap.algotutorbe.judge.dto.JudgeResult;
import org.rap.algotutorbe.submission.SubmissionCreatedMessage;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class JudgeService {

    public JudgeResult processSubmission(SubmissionCreatedMessage message) {
        log.info("Processing submission: submissionId={}, problemId={}, language={}",
                message.submissionId(), message.problemId(), message.programmingLanguage());
        // TODO: Implement actual judging logic (fetch testcases, call Piston, save results)
        return null;
    }
}

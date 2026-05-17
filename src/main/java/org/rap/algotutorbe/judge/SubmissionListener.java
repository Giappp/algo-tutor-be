package org.rap.algotutorbe.judge;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rap.algotutorbe.common.config.RabbitMQConfig;
import org.rap.algotutorbe.judge.dto.JudgeResult;
import org.rap.algotutorbe.submission.dto.SubmissionCreatedMessage;
import org.rap.algotutorbe.submission.entities.Submission;
import org.rap.algotutorbe.submission.entities.Verdict;
import org.rap.algotutorbe.submission.service.SubmissionService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubmissionListener {
    private final JudgeService judgeService;
    private final SubmissionService submissionService;
    private final LessonProgressUpdater lessonProgressUpdater;

    @RabbitListener(queues = RabbitMQConfig.SUBMISSION_CREATED_QUEUE)
    public void handleSubmissionCreated(SubmissionCreatedMessage message) {
        log.info("Received SubmissionCreatedMessage from RabbitMQ: submissionId={}", message.submissionId());

        try {
            JudgeResult result = judgeService.processSubmission(message);

            // Auto-update lesson progress if ACCEPTED
            if (result.verdict() == Verdict.ACCEPTED) {
                Submission submission = submissionService.getOrThrowSubmission(message.submissionId());
                lessonProgressUpdater.markLessonCompletedIfNeeded(submission);
            }
        } catch (Exception e) {
            log.error("Fatal error processing submissionId={}: {}", message.submissionId(), e.getMessage(), e);
            submissionService.markAsSystemError(message.submissionId(), e.getMessage());
        }
    }
}

package org.rap.algotutorbe.judge.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rap.algotutorbe.common.config.RabbitMQConfig;
import org.rap.algotutorbe.judge.application.services.JudgeService;
import org.rap.algotutorbe.submission.SubmissionCreatedMessage;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubmissionListener {
    private final JudgeService judgeService;

    @RabbitListener(queues = RabbitMQConfig.SUBMISSION_EXCHANGE)
    public void handleSubmissionCreated(SubmissionCreatedMessage message) {
        log.info("Received SubmissionCreatedMessage from RabbitMQ: submissionId={}", message.submissionId());

        try {
            judgeService.processSubmission(message);
        } catch (Exception e) {
            // Xử lý khi có lỗi ném ra từ Service (tránh việc message bị requeue liên tục gây kẹt hệ thống)
            log.error("Fatal error processing submissionId={}: {}", message.submissionId(), e.getMessage(), e);

            // Tùy chọn: Bạn có thể cập nhật trạng thái SYSTEM_ERROR vào DB ở đây nếu message bị lỗi cứng
        }
    }
}

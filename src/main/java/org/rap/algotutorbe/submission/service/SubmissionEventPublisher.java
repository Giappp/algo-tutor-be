package org.rap.algotutorbe.submission.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rap.algotutorbe.common.config.RabbitMQConfig;
import org.rap.algotutorbe.submission.dto.SubmissionCreatedMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubmissionEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishSubmissionCreated(SubmissionCreatedMessage message) {
        log.info("Publishing SubmissionCreatedMessage: submissionId={}", message.submissionId());
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.SUBMISSION_EXCHANGE,
                RabbitMQConfig.ROUTING_CREATED,
                message
        );
    }
}
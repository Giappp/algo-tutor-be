package org.rap.algotutorbe.submission.internal.infrastructure;

import lombok.RequiredArgsConstructor;
import org.rap.algotutorbe.judge.JudgeRequestMessage;
import org.rap.algotutorbe.submission.SubmissionCreatedEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SubmissionEventListener {
    private final RabbitTemplate rabbitTemplate;

    @Value("${algotutor.rabbitmq.exchange.judge}")
    private String judgeExchange;

    @Value("${algotutor.rabbitmq.routing-key.judge}")
    private String judgeRoutingKey;

    @ApplicationModuleListener
    void onSubmissionCreated(SubmissionCreatedEvent event) {
        JudgeRequestMessage message = new JudgeRequestMessage(
                event.submissionId(),
                event.problemId(),
                event.sourceCode(),
                event.language()
        );

        rabbitTemplate.convertAndSend(judgeExchange, judgeRoutingKey, message);
    }
}

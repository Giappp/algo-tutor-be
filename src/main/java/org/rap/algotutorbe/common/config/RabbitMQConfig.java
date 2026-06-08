package org.rap.algotutorbe.common.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // ── Exchange ──────────────────────────────────────
    public static final String SUBMISSION_EXCHANGE = "submission.exchange";

    // ── Queues ────────────────────────────────────────
    public static final String SUBMISSION_CREATED_QUEUE = "submission.created.queue";
    public static final String SUBMISSION_JUDGED_QUEUE = "submission.judged.queue";
    public static final String SUBMISSION_DLQ = "submission.dlq";

    // ── Routing Keys ──────────────────────────────────
    public static final String ROUTING_CREATED = "submission.created";
    public static final String ROUTING_JUDGED = "submission.judged";

    // ── Dead Letter ───────────────────────────────────
    private static final String DL_EXCHANGE = "submission.dlx";

    @Bean
    public TopicExchange submissionExchange() {
        return ExchangeBuilder
                .topicExchange(SUBMISSION_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return ExchangeBuilder
                .directExchange(DL_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public Queue submissionCreatedQueue() {
        return QueueBuilder
                .durable(SUBMISSION_CREATED_QUEUE)
                .withArgument("x-dead-letter-exchange", DL_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", SUBMISSION_DLQ)
                .withArgument("x-message-ttl", 300_000)
                .build();
    }

    @Bean
    public Queue submissionJudgedQueue() {
        return QueueBuilder
                .durable(SUBMISSION_JUDGED_QUEUE)
                .withArgument("x-dead-letter-exchange", DL_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", SUBMISSION_DLQ)
                .build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder
                .durable(SUBMISSION_DLQ)
                .build();
    }

    @Bean
    public Binding bindingCreated(
            Queue submissionCreatedQueue,
            TopicExchange submissionExchange
    ) {
        return BindingBuilder
                .bind(submissionCreatedQueue)
                .to(submissionExchange)
                .with(ROUTING_CREATED);
    }

    @Bean
    public Binding bindingJudged(
            Queue submissionJudgedQueue,
            TopicExchange submissionExchange
    ) {
        return BindingBuilder
                .bind(submissionJudgedQueue)
                .to(submissionExchange)
                .with(ROUTING_JUDGED);
    }

    @Bean
    public Binding deadLetterBinding(
            Queue deadLetterQueue,
            DirectExchange deadLetterExchange
    ) {
        return BindingBuilder
                .bind(deadLetterQueue)
                .to(deadLetterExchange)
                .with(SUBMISSION_DLQ);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter
    ) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter
    ) {
        SimpleRabbitListenerContainerFactory factory =
                new SimpleRabbitListenerContainerFactory();

        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);

        // Lỗi khi consume message -> không requeue lại vô hạn,
        // message sẽ đi DLQ nếu queue đã cấu hình dead-letter.
        factory.setDefaultRequeueRejected(false);

        factory.setPrefetchCount(5);

        return factory;
    }
}
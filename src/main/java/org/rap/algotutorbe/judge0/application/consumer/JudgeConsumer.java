package org.rap.algotutorbe.judge0.application.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rap.algotutorbe.common.config.RabbitMQConfig;
import org.rap.algotutorbe.judge0.application.services.Judge0Service;
import org.rap.algotutorbe.judge0.application.utils.Judge0PayloadBuilder;
import org.rap.algotutorbe.problem.application.services.ProblemService;
import org.rap.algotutorbe.submission.SubmissionCreatedMessage;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class JudgeConsumer {
    private final ProblemService problemService;
    private final Judge0Service judge0Service;
    private final Judge0PayloadBuilder payloadBuilder;

    @RabbitListener(queues = RabbitMQConfig.SUBMISSION_CREATED_QUEUE)
    public void consume(SubmissionCreatedMessage message) {
        log.info("Nhận message submissionId={}, language={}",
                message.submissionId(), message.language());

        int languageId = resolveLanguageId(message.language());
        var testcases = problemService.getProblemTestcase(message.problemId());
        judge0Service.processAndSendToJudge0(
                message.submissionId(),
                message.sourceCode(),
                languageId,
                testcases
        );
    }

    private int resolveLanguageId(String language) {
        return switch (language.toUpperCase()) {
            case "JAVA" -> 62;
            case "PYTHON" -> 71;
            case "CPP", "C_PLUS_PLUS" -> 54;
            default -> throw new IllegalArgumentException(
                    "Ngôn ngữ không hỗ trợ: " + language);
        };
    }
}

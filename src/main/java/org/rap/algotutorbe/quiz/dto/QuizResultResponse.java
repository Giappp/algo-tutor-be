package org.rap.algotutorbe.quiz.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record QuizResultResponse(
        UUID attemptId,
        Long quizId,
        String quizTitle,
        Integer score,
        Integer totalPoints,
        Integer percentage,
        boolean passed,
        Integer passingScore,
        Instant startedAt,
        Instant completedAt,
        Integer timeSpentSeconds,
        List<QuizAnswerResult> answers
) {
}

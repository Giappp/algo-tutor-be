package org.rap.algotutorbe.quiz.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record QuizAttemptResponse(
        UUID id,
        Long quizId,
        String quizTitle,
        Integer score,
        Integer totalPoints,
        Boolean passed,
        Instant startedAt,
        Instant completedAt,
        Integer timeSpentSeconds
) {
}

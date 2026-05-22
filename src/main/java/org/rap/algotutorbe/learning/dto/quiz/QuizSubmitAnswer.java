package org.rap.algotutorbe.learning.dto.quiz;

import org.rap.algotutorbe.learning.models.QuestionAnswer;

import java.time.Instant;
import java.util.List;

public record QuizSubmitAnswer(List<QuestionAnswer> answers, Instant startedAt, Integer timeSpentSeconds,
                               Instant completedAt) {
}
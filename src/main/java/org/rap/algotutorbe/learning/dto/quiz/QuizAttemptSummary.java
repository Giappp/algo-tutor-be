package org.rap.algotutorbe.learning.dto.quiz;

import java.time.Instant;
import java.util.UUID;

public record QuizAttemptSummary(UUID id, Integer attemptNumber, Float score, Boolean passed, Integer correctCount,
                                 Integer totalQuestions, Instant completedAt, Integer timeSpentSeconds) {
}

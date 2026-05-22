package org.rap.algotutorbe.learning.dto.quiz;

import org.rap.algotutorbe.learning.models.QuestionResult;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record QuizAttemptResponse(UUID id, Float score, Boolean passed, Integer correctCount, Integer attemptNumber,
                                  Instant completedAt, List<QuestionResult> questionResults,
                                  Boolean lessonProgressUpdated) {
}
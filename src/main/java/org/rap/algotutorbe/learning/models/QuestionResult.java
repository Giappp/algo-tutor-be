package org.rap.algotutorbe.learning.models;

import java.util.List;

public record QuestionResult(Long questionId, Boolean isCorrect, List<String> correctOptionIds) {
}

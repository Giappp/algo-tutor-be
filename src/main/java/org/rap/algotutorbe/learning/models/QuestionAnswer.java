package org.rap.algotutorbe.learning.models;

import java.util.List;

public record QuestionAnswer(Long questionId, List<String> selectedOptionIds) {
}
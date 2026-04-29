package org.rap.algotutorbe.learning.dto;

import org.rap.algotutorbe.learning.mapper.QuestionType;

import java.util.List;

public record QuizQuestionDTO(String question, QuestionType type, Integer points, String explanation,
                              List<QuestionChoiceDTO> choices) {
}

package org.rap.algotutorbe.learning.dto;

import org.rap.algotutorbe.learning.models.QuestionType;

import java.util.List;

public record QuizQuestionResponseDTO(
        Long id,
        String question,
        QuestionType type,
        Integer points,
        String explanation,
        Integer orderIndex,
        List<QuizChoiceDTO> choices
) {
}

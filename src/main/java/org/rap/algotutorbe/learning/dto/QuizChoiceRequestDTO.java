package org.rap.algotutorbe.learning.dto;

public record QuizChoiceRequestDTO(
        String text,
        Boolean isCorrect,
        String explanation
) {
}

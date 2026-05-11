package org.rap.algotutorbe.learning.dto;

public record QuizChoiceResponseDTO(
        String text,
        Boolean isCorrect,
        String explanation
) {
}

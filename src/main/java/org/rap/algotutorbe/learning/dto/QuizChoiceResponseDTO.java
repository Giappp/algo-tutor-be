package org.rap.algotutorbe.learning.dto;

public record QuizChoiceResponseDTO(
        String id,
        String text,
        Boolean isCorrect,
        String explanation
) {
}

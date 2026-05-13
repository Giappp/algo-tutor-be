package org.rap.algotutorbe.learning.dto;

public record TheoryLessonResponseDTO(
        Long id,
        String title,
        String content,
        String type,
        Integer displayOrder,
        String difficulty
) {
}

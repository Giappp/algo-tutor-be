package org.rap.algotutorbe.learning.dto;

public record TheoryLessonResponseDTO(
        Long id,
        String title,
        String content,
        String type,
        Integer orderIndex,
        Boolean isPublished,
        String difficulty
) {
}

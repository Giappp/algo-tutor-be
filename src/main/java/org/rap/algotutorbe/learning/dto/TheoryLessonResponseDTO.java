package org.rap.algotutorbe.learning.dto;

public record TheoryLessonResponseDTO(
        Long id,
        String title,
        String slug,
        String content,
        Integer orderIndex,
        Boolean isPublished,
        String difficulty,
        Long topicId,
        Long learningPathId
) {
}

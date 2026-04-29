package org.rap.algotutorbe.learning.dto;

import org.rap.algotutorbe.learning.enums.Difficulty;
import org.rap.algotutorbe.learning.enums.LessonType;

import java.time.Instant;

public record LessonResponseDTO(
        Long id,
        String title,
        String slug,
        LessonType type,
        String content,
        Integer orderIndex,
        Boolean isPublished,
        Difficulty difficulty,
        Instant createdAt,
        Instant updatedAt
) {
}

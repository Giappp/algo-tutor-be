package org.rap.algotutorbe.learning.dto;

import java.time.Instant;
import java.util.List;

public record TopicResponseDTO(
        Long id,
        String name,
        String description,
        Integer displayOrder,
        Long learningPathId,
        int lessonCount,
        Instant createdAt,
        Instant updatedAt,
        List<LessonResponseDTO> lessons
) {
}

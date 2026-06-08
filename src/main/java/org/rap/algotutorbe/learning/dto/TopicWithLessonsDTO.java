package org.rap.algotutorbe.learning.dto;

import java.time.Instant;
import java.util.List;

public record TopicWithLessonsDTO(
        Long id,
        String name,
        String description,
        Integer displayOrder,
        Integer lessonCount,
        Boolean unlocked,
        Boolean completed,
        Integer completedLessons,
        Integer totalLessons,
        Instant createdAt,
        Instant updatedAt,
        List<LessonWithProgressDTO> lessons
) {
}

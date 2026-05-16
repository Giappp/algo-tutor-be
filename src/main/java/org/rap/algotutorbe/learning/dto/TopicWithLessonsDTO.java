package org.rap.algotutorbe.learning.dto;

import java.time.Instant;
import java.util.List;

public record TopicWithLessonsDTO(
        Long id,
        String name,
        String description,
        Integer displayOrder,
        Boolean isLocked,
        Integer lessonCount,
        Instant createdAt,
        Instant updatedAt,
        List<LessonWithProgressDTO> lessons
) {}

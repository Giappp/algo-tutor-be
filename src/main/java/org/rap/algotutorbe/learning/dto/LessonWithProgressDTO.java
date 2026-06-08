package org.rap.algotutorbe.learning.dto;

import org.rap.algotutorbe.learning.enums.Difficulty;
import org.rap.algotutorbe.learning.enums.LessonType;
import org.rap.algotutorbe.learning.enums.ProgressStatus;

import java.time.Instant;

public record LessonWithProgressDTO(
        Long id,
        String title,
        String slug,
        LessonType type,
        Integer displayOrder,
        Difficulty difficulty,
        ProgressStatus status,
        Boolean unlocked,
        Instant createdAt,
        Instant updatedAt
) {
}

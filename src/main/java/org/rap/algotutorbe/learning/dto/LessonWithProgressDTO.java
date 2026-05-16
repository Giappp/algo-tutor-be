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
        ProgressStatus progress,
        Instant createdAt,
        Instant updatedAt
) {}

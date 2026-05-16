package org.rap.algotutorbe.learning.dto;

import org.rap.algotutorbe.learning.enums.ProgressStatus;

import java.time.Instant;

public record LessonProgressionDTO(
        Long lessonId,
        ProgressStatus status,
        Instant updatedAt
) {}

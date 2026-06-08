package org.rap.algotutorbe.learning.dto;

import org.rap.algotutorbe.learning.enums.ProgressStatus;

import java.time.Instant;

public record LessonProgressUpdateResponse(
        Long lessonId,
        Long roadmapId,
        ProgressStatus status,
        Instant updatedAt
) {
}

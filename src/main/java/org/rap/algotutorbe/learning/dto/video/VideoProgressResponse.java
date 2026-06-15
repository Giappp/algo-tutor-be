package org.rap.algotutorbe.learning.dto.video;

import org.rap.algotutorbe.learning.enums.ProgressStatus;

import java.time.Instant;

public record VideoProgressResponse(
        Long lessonId,
        String lessonSlug,
        Integer durationSeconds,
        Integer positionSeconds,
        Integer watchedSeconds,
        Double watchedPercentage,
        ProgressStatus status,
        Boolean completed,
        Instant updatedAt
) {
}

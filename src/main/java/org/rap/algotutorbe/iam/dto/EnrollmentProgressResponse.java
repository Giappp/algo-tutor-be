package org.rap.algotutorbe.iam.dto;

import java.time.Instant;
import java.util.UUID;

public record EnrollmentProgressResponse(
        UUID enrollmentId,
        Long roadmapId,
        String roadmapName,
        String roadmapSlug,
        String thumbnailUrl,
        String roadmapLevel,
        String status,
        double completionPercentage,
        Instant enrolledAt,
        Instant completedAt,
        String nextLessonSlug,
        String nextLessonTitle
) {
}

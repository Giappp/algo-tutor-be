package org.rap.algotutorbe.iam.dto;

public record CurrentLessonResponse(
        String roadmapSlug,
        String lessonSlug,
        String lessonTitle,
        String roadmapName,
        int completionPercentage
) {
}

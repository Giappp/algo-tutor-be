package org.rap.algotutorbe.iam.dto;

public record EnrollmentProgressResponse(
        String roadmapName,
        String roadmapSlug,
        int completionPercentage,
        String nextLessonSlug,
        String nextLessonTitle,
        String thumbnailUrl
) {
}

package org.rap.algotutorbe.iam.dto;

public record UserRoadMapResponse(String roadmapName, String roadmapSlug, Integer completePercentage,
                                  String nextLessonSlug, String nextLessonTitle) {
}

package org.rap.algotutorbe.learning.dto;

import java.time.Instant;
import java.util.List;

public record RoadmapDetailResponseDTO(
        Long id,
        String name,
        String slug,
        String level,
        String description,
        String goal,
        String thumbnailUrl,
        Boolean isPublished,
        Boolean isPremium,
        Integer enrollmentCount,
        Integer topicCount,
        Integer lessonCount,
        Boolean enrolled,
        Instant createdAt,
        Instant updatedAt,
        List<TopicWithLessonsDTO> topics
) {
}

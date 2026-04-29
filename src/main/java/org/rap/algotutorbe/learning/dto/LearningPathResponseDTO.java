package org.rap.algotutorbe.learning.dto;

import org.rap.algotutorbe.learning.enums.Level;

import java.time.Instant;
import java.util.List;

public record LearningPathResponseDTO(
        Long id,
        String name,
        String slug,
        Level level,
        String description,
        String goal,
        String thumbnailUrl,
        boolean deleted,
        int topicCount,
        int totalLessonCount,
        int publishedLessonCount,
        int enrollmentCount,
        Instant createdAt,
        Instant updatedAt,
        List<TopicResponseDTO> topics
) {
}

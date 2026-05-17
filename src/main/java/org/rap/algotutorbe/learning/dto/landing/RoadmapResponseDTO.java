package org.rap.algotutorbe.learning.dto.landing;

public record RoadmapResponseDTO(String name,
                                 String slug,
                                 String level,
                                 String thumbnailUrl,
                                 String description,
                                 String goal,
                                 Boolean isPremium,
                                 Integer topicCount,
                                 Integer lessonCount,
                                 Integer enrollmentCount
) {
}

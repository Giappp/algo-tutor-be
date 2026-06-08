package org.rap.algotutorbe.ai.dto;

import java.util.List;
import java.util.UUID;

public record AiRoadmapAdvisoryResponse(
        UUID conversationId,
        List<RoadmapInfo> roadmaps
) {
    public record RoadmapInfo(
            String name,
            String slug,
            String level,
            String description,
            String thumbnailUrl,
            int topicCount,
            int lessonCount,
            boolean isPremium
    ) {}
}

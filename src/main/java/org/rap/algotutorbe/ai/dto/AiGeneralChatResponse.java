package org.rap.algotutorbe.ai.dto;

import java.util.List;
import java.util.UUID;
import org.rap.algotutorbe.ai.dto.AiRoadmapAdvisoryResponse.RoadmapInfo;

public record AiGeneralChatResponse(
        UUID conversationId,
        String answer,
        List<RoadmapInfo> roadmaps
) {
}

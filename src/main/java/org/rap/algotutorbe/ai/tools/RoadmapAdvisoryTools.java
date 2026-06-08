package org.rap.algotutorbe.ai.tools;

import lombok.Getter;
import lombok.Setter;
import org.rap.algotutorbe.ai.dto.AiRoadmapAdvisoryResponse;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Getter
@Setter
public class RoadmapAdvisoryTools {
    private final List<AiRoadmapAdvisoryResponse.RoadmapInfo> availableRoadmaps;
    private final List<String> recommendedSlugs = new CopyOnWriteArrayList<>();

    public RoadmapAdvisoryTools(List<AiRoadmapAdvisoryResponse.RoadmapInfo> availableRoadmaps) {
        this.availableRoadmaps = availableRoadmaps;
    }

    @org.springframework.ai.tool.annotation.Tool(
            description = "Get list of all available learning paths (roadmaps) on the platform with their details so you can choose which ones to recommend."
    )
    public List<AiRoadmapAdvisoryResponse.RoadmapInfo> getAvailableRoadmaps() {
        return availableRoadmaps;
    }

    @org.springframework.ai.tool.annotation.Tool(
            description = "Recommend specific learning roadmaps to the user based on their needs. You must provide the slugs of the roadmaps you are recommending."
    )
    public void recommendRoadmaps(List<String> slugs) {
        if (slugs != null) {
            recommendedSlugs.addAll(slugs);
        }
    }

    public List<String> getRecommendedSlugs() {
        return recommendedSlugs;
    }
}


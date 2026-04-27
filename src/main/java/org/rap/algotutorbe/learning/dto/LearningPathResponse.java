package org.rap.algotutorbe.learning.dto;

import java.util.List;

public record LearningPathResponse(String name, String slug, String description, String goal, String thumbnailUrl,
                                   List<TopicDto> topics) {
}

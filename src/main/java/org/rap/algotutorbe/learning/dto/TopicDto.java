package org.rap.algotutorbe.learning.dto;

import java.util.List;

public record TopicDto(String name, String description, Integer orderIndex, String scopeTags, Boolean isLocked,
                       List<LessonDto> lessons) {
}

package org.rap.algotutorbe.ai.dto;

import org.rap.algotutorbe.learning.enums.LessonType;
import java.util.List;

public record AdminLessonContentGenerateResponse(
        Long lessonId,
        LessonType lessonType,
        Object content,
        GenerationContext context,
        Integer inputTokens,
        Integer outputTokens
) {
    public record GenerationContext(
            Long learningPathId,
            String learningPathName,
            Long topicId,
            String topicName,
            List<String> siblingLessons
    ) {
    }
}

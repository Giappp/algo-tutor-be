package org.rap.algotutorbe.ai.dto;

public record AiQuestionSourceResponse(
        Long lessonId,
        String title,
        Long topicId,
        String topicName,
        Integer displayOrder,
        Integer estimatedMinutes,
        Integer contentCharacterCount,
        String contentPreview,
        Boolean isPublished
) {
}

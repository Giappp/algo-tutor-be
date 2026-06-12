package org.rap.algotutorbe.ai.dto;

import org.rap.algotutorbe.learning.models.QuestionType;

import java.util.List;

public record GenerateQuestionsFromSourcesResponse(
        Long quizLessonId,
        List<DraftQuestion> questions,
        GenerationContext context,
        Integer inputTokens,
        Integer outputTokens
) {
    public record DraftQuestion(
            String question,
            QuestionType type,
            Integer points,
            Integer orderIndex,
            String explanation,
            List<DraftChoice> choices
    ) {
    }

    public record DraftChoice(String text, Boolean isCorrect, String explanation) {
    }

    public record GenerationContext(List<Source> sources, List<Long> truncatedSourceIds) {
    }

    public record Source(Long lessonId, String title, String topicName, Boolean isPublished) {
    }
}

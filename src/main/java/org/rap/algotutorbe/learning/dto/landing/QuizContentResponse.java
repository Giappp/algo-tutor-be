package org.rap.algotutorbe.learning.dto.landing;

import java.util.List;

/**
 * Public response DTO for quiz lesson content.
 * Matches the frontend spec: GET /lessons/:slug/quiz
 */
public record QuizContentResponse(
        Long id,
        String slug,
        String title,
        Integer passingScore,
        List<QuizQuestionItem> questions
) {
    public record QuizQuestionItem(
            Long id,
            String text,
            String type,
            List<QuizOptionItem> options,
            String explanation,
            List<String> correctOptionIds
    ) {
    }

    public record QuizOptionItem(
            String id,
            String text
    ) {
    }
}

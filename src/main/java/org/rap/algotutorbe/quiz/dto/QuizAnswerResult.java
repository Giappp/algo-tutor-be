package org.rap.algotutorbe.quiz.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record QuizAnswerResult(
        Long questionId,
        String question,
        String givenAnswer,
        String correctAnswer,
        boolean correct,
        Integer pointsEarned,
        Integer maxPoints,
        String explanation
) {
}

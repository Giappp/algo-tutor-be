package org.rap.algotutorbe.quiz.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record QuizResponse(
        Long id,
        String title,
        String description,
        Integer passingScore,
        Integer timeLimitMinutes,
        Integer questionCount,
        Integer totalPoints,
        List<QuizQuestionResponse> questions
) {
}

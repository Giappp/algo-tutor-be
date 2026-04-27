package org.rap.algotutorbe.quiz.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record QuizQuestionResponse(
        Long id,
        String question,
        String type,
        List<String> options,
        Integer points,
        Integer orderIndex
) {
}

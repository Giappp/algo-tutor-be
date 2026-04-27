package org.rap.algotutorbe.quiz.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record QuizAnswerSnapshot(
        @JsonProperty("questionId") Long questionId,
        @JsonProperty("selectedChoiceIndex") int selectedChoiceIndex,
        @JsonProperty("pointsEarned") int pointsEarned
) {
}

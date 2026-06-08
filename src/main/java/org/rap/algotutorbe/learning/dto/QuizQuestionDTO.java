package org.rap.algotutorbe.learning.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.rap.algotutorbe.learning.models.QuestionType;

import java.util.List;

public record QuizQuestionDTO(@NotBlank String question, @NotNull QuestionType type, Integer points, String explanation,
                              Integer orderIndex,
                              @Valid
                              @NotNull
                              @Size(min = 2, message = "{errors.quiz.question.choices.size}")
                              List<QuizChoiceDTO> choices) {
}

package org.rap.algotutorbe.learning.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record QuestionChoiceDTO(@NotNull @NotBlank String text, @NotNull Boolean isCorrect, String explanation) {
}

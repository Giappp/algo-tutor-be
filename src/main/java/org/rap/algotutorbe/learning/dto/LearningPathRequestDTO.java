package org.rap.algotutorbe.learning.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.rap.algotutorbe.learning.enums.Level;

public record LearningPathRequestDTO(@NotNull @NotBlank String name,
                                     @NotNull @NotBlank String description, @NotNull @NotBlank String goal,
                                     @NotNull Boolean isPremium,
                                     String thumbnailUrl,
                                     @NotNull Level level) {
}
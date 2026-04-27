package org.rap.algotutorbe.learning.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.rap.algotutorbe.learning.models.Level;

public record CreateLearningPathRequest(
        @NotBlank(message = "name.required")
        @Size(max = 255, message = "name.max")
        String name,

        @Size(max = 255, message = "slug.max")
        String slug,

        @NotNull(message = "level.required")
        Level level,

        String description,

        String goal,

        @Pattern(regexp = "^https?://.*$", message = "thumbnailUrl.invalid")
        String thumbnailUrl
) {
}

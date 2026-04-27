package org.rap.algotutorbe.learning.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.rap.algotutorbe.learning.models.Level;

public record UpdateLearningPathRequest(
        @Size(max = 255, message = "name.max")
        String name,
        @Size(max = 255, message = "slug.max")
        String slug,

        Level level,

        String description,

        String goal,

        @Pattern(regexp = "^https?://.*$", message = "thumbnailUrl.invalid")
        String thumbnailUrl
) {
}

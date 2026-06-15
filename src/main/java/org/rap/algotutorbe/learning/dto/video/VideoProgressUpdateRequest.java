package org.rap.algotutorbe.learning.dto.video;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record VideoProgressUpdateRequest(
        @NotNull @PositiveOrZero Integer positionSeconds,
        @NotNull @PositiveOrZero Integer watchedDeltaSeconds
) {
}

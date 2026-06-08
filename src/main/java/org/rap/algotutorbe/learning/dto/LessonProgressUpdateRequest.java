package org.rap.algotutorbe.learning.dto;

import jakarta.validation.constraints.NotNull;
import org.rap.algotutorbe.learning.enums.ProgressStatus;

public record LessonProgressUpdateRequest(
        @NotNull ProgressStatus status
) {}

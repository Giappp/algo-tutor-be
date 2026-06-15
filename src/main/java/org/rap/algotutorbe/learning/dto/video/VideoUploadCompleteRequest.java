package org.rap.algotutorbe.learning.dto.video;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record VideoUploadCompleteRequest(
        @NotBlank String uploadId,
        @NotBlank String objectKey,
        @NotNull @Positive Integer durationSeconds,
        @NotEmpty List<@Valid CompletedVideoPart> parts
) {
    public record CompletedVideoPart(
            @NotNull @Positive Integer partNumber,
            @NotBlank String eTag
    ) {
    }
}

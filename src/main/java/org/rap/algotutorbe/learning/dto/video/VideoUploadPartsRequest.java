package org.rap.algotutorbe.learning.dto.video;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record VideoUploadPartsRequest(
        @NotBlank String uploadId,
        @NotBlank String objectKey,
        @NotEmpty List<@NotNull @Positive Integer> partNumbers
) {
}

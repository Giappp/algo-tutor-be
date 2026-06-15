package org.rap.algotutorbe.learning.dto.video;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record VideoUploadInitiateRequest(
        @NotBlank String fileName,
        @NotBlank String contentType,
        @NotNull @Positive Long fileSize
) {
}

package org.rap.algotutorbe.learning.dto.video;

import jakarta.validation.constraints.NotBlank;

public record VideoUploadAbortRequest(
        @NotBlank String uploadId,
        @NotBlank String objectKey
) {
}

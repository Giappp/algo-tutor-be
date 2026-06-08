package org.rap.algotutorbe.iam.application.dto;

import jakarta.validation.constraints.NotBlank;

public record BlockUserRequest(
        @NotBlank String reason
) {}

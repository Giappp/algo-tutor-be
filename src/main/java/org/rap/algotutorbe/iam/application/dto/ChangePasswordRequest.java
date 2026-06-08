package org.rap.algotutorbe.iam.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
    @NotBlank String oldPassword,
    @NotBlank @Size(min = 6, max = 100) String newPassword,
    @NotBlank String confirmPassword
) {}

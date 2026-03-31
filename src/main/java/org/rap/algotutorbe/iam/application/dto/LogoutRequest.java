package org.rap.algotutorbe.iam.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LogoutRequest(@NotNull @NotBlank String refreshToken) {
}


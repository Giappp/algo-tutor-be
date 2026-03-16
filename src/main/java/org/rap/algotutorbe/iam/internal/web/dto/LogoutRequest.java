package org.rap.algotutorbe.iam.internal.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LogoutRequest(@NotNull @NotBlank String refreshToken) {
}


package org.rap.algotutorbe.iam.internal.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RefreshTokenRequest(@NotNull @NotBlank String refreshToken) {
}

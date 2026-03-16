package org.rap.algotutorbe.iam.internal.web.dto;

import lombok.Builder;

@Builder
public record TokenResponse(String accessToken, String refreshToken) {
}


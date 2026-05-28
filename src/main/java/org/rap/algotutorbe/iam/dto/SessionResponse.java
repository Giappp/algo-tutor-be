package org.rap.algotutorbe.iam.dto;

import java.time.Instant;

public record SessionResponse(
    Long id,
    String ipAddress,
    String deviceInfo,
    Instant createdAt,
    Instant expiresAt,
    boolean isCurrent
) {}

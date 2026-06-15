package org.rap.algotutorbe.learning.dto.landing;

import java.time.Instant;

public record VideoContentResponse(
        Long id,
        String slug,
        String title,
        String description,
        Integer durationSeconds,
        String playbackUrl,
        Instant expiresAt
) {
}

package org.rap.algotutorbe.learning.dto.landing;

/**
 * Public response DTO for theory lesson content.
 * Matches the frontend spec: GET /lessons/:slug/theory
 */
public record TheoryContentResponse(
        Long id,
        String slug,
        String title,
        String content,
        Integer estimatedMinutes
) {
}

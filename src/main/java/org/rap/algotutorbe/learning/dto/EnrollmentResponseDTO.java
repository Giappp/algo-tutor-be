package org.rap.algotutorbe.learning.dto;

import org.rap.algotutorbe.learning.enums.EnrollmentStatus;

import java.time.Instant;
import java.util.UUID;

public record EnrollmentResponseDTO(
        String id,
        UUID userId,
        Long learningPathId,
        String learningPathName,
        EnrollmentStatus status,
        Instant completedAt,
        Instant enrolledAt
) {
}

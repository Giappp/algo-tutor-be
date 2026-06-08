package org.rap.algotutorbe.learning.dto;

import org.rap.algotutorbe.learning.enums.EnrollmentStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record EnrollmentDetailResponseDTO(
        UUID id,
        UUID userId,
        Long learningPathId,
        String learningPathName,
        EnrollmentStatus status,
        Instant completedAt,
        Instant createdAt,
        List<LessonProgressionDTO> lessonProgressions
) {}

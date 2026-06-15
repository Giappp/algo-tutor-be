package org.rap.algotutorbe.learning.dto;

import org.rap.algotutorbe.learning.enums.VideoProcessingStatus;

public record VideoLessonResponseDTO(
        Long id,
        String title,
        Boolean isPublished,
        String type,
        Integer displayOrder,
        String difficulty,
        String description,
        String sourceObjectKey,
        String thumbnailObjectKey,
        Integer durationSeconds,
        Long fileSizeBytes,
        String mimeType,
        VideoProcessingStatus processingStatus
) {
}

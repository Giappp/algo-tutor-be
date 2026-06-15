package org.rap.algotutorbe.learning.dto.video;

import org.rap.algotutorbe.learning.enums.VideoProcessingStatus;

public record VideoUploadCompleteResponse(
        Long lessonId,
        String objectKey,
        Long fileSize,
        String contentType,
        Integer durationSeconds,
        VideoProcessingStatus processingStatus
) {
}

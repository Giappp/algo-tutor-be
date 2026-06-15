package org.rap.algotutorbe.learning.dto.video;

public record VideoUploadInitiateResponse(
        String uploadId,
        String objectKey,
        long partSize,
        int totalParts
) {
}

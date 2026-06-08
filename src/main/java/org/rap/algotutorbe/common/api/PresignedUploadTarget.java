package org.rap.algotutorbe.common.api;

public record PresignedUploadTarget(
        String objectKey,
        String uploadUrl,
        String fileUrl
) {
}
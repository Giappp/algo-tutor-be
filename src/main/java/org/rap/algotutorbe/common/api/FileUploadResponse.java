package org.rap.algotutorbe.common.api;

public record FileUploadResponse(String url, String fileName, Long fileSize, String mimeType) {
}

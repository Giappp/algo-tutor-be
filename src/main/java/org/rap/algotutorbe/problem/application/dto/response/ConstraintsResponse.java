package org.rap.algotutorbe.problem.application.dto.response;

public record ConstraintsResponse(
        Long timeLimitMs,
        Long memoryLimitMb,
        Long maxCodeLengthBytes,
        Long maxOutputSizeBytes
) {
}

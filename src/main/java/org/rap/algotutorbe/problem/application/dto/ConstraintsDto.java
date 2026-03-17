package org.rap.algotutorbe.problem.application.dto;

public record ConstraintsDto (Long timeLimitMs,
                         Long memoryLimitMb,
                         Long maxCodeLengthBytes,
                         Long maxOutputSizeBytes) {
}

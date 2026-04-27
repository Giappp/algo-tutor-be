package org.rap.algotutorbe.problem.dto;

public record ConstraintsDto(Long timeLimitMs,
                             Long memoryLimitMb,
                             Long maxCodeLengthBytes,
                             Long maxOutputSizeBytes) {
}

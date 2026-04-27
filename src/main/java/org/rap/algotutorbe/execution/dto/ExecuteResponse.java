package org.rap.algotutorbe.execution.dto;

public record ExecuteResponse(
        String stdout,
        String stderr,
        int exitCode,
        Integer executionTime,
        Integer memoryUsed
) {
}


package org.rap.algotutorbe.judge.application.dto;


import java.util.List;

public record PistonRequest(
        String language,
        String version,
        List<PistonFile> files,
        String stdin,
        int runTimeoutMs,
        int memoryLimitMb
) {
}
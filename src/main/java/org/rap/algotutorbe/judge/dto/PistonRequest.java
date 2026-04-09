package org.rap.algotutorbe.judge.dto;


import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record PistonRequest(
        String language,
        String version,
        List<PistonFile> files,
        String stdin,
        @JsonProperty("run_timeout")
        int runTimeout,
        @JsonProperty("compile_timeout")
        int compileTimeout,
        @JsonProperty("memory_limit_mb")
        int memoryLimitMb
) {
}
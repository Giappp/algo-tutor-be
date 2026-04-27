package org.rap.algotutorbe.judge.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PistonStage(
        String stdout,
        String stderr,
        String output,
        int code,
        String signal,
        @JsonProperty("memory") Integer memory,
        @JsonProperty("cpu_time") Integer cpuTime,
        @JsonProperty("wall_time") Integer wallTime
) {
}
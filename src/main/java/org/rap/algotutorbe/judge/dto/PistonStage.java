package org.rap.algotutorbe.judge.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PistonStage(
        String stdout,
        String stderr,
        String output,
        int code,
        String signal
) {
}
package org.rap.algotutorbe.judge.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PistonResponse(
        String language,
        String version,
        PistonStage compile,
        PistonStage run
) {
}
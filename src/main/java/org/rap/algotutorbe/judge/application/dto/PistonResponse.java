package org.rap.algotutorbe.judge.application.dto;

public record PistonResponse(
        String language,
        String version,
        PistonResult compile,
        PistonResult run
) {
}
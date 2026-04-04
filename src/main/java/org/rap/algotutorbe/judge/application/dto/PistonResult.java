package org.rap.algotutorbe.judge.application.dto;

public record PistonResult(
        String stdout,
        String stderr,
        int code,
        String signal,
        String output
) {
}

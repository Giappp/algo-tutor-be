package org.rap.algotutorbe.judge.dto;

public record PistonResult(
        String stdout,
        String stderr,
        int code,
        String signal,
        String output
) {
}

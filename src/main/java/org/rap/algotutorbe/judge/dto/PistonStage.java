package org.rap.algotutorbe.judge.dto;

public record PistonStage(
        String stdout,
        String stderr,
        int code,
        String signal
) {
}
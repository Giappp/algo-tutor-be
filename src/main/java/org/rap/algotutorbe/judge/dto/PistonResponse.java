package org.rap.algotutorbe.judge.dto;

public record PistonResponse(
        PistonStage compile,
        PistonStage run
) {
}
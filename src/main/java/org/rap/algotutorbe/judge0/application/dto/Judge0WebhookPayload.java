package org.rap.algotutorbe.judge0.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Judge0WebhookPayload(
        String token,
        String stdout,
        String stderr,
        @JsonProperty("compile_output") String compileOutput,
        String message,
        Judge0Status status,
        Double time,
        Integer memory
) {
    public record Judge0Status(
            int id,
            String description
    ) {
    }
}
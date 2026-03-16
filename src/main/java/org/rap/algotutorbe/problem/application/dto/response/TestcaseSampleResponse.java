package org.rap.algotutorbe.problem.application.dto.response;

public record TestcaseSampleResponse(
        Long id,
        String input,
        String expectedOutput,
        String explanation
) {
}
package org.rap.algotutorbe.problem.dto.response.testcase;

public record TestcaseSampleResponse(
        Long id,
        String stdin,
        String expectedStdout,
        String explanation
) {
}
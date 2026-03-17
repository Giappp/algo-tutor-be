package org.rap.algotutorbe.problem.application.dto.request;

public record TestcaseRequest(Long id,
                              String input,
                              String expectedOutput,
                              boolean isSample,
                              int orderIndex,
                              String explanation
) {
}

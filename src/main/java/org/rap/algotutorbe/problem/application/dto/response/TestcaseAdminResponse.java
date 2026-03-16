package org.rap.algotutorbe.problem.application.dto.response;

public record TestcaseAdminResponse(Long id,
                                    String input,
                                    String expectedOutput,
                                    boolean isSample,
                                    int orderIndex,
                                    String explanation) {
}

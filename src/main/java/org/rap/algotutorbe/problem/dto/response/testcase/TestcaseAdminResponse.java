package org.rap.algotutorbe.problem.dto.response.testcase;

public record TestcaseAdminResponse(
        Long id,
        String stdin,
        String expectedStdout,
        boolean isSample,
        int orderIndex,
        String explanation
) {
}

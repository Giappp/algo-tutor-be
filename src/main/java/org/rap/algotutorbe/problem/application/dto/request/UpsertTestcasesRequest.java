package org.rap.algotutorbe.problem.application.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpsertTestcasesRequest(
        @NotNull java.util.List<TestcaseRequest> testcases
) {
}

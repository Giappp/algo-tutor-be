package org.rap.algotutorbe.problem.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record TestcasesRequest(
        @NotNull @Size(min = 1) List<TestcaseRequest> testCases
) {
}

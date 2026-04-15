package org.rap.algotutorbe.problem.application.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.rap.algotutorbe.problem.domain.enums.ProgrammingLanguage;

import java.util.List;

public record RunTestcasesRequest(
        @NotNull @Size(min = 1) List<TestcaseRequest> testCases,
        @NotNull ProgrammingLanguage language,
        @NotNull String authorSolution
) {
}

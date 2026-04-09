package org.rap.algotutorbe.problem.application.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.rap.algotutorbe.problem.domain.enums.ProgrammingLanguage;

import java.util.List;

public record RunTestcasesRequest(
        @NotNull @Min(value = 1) List<TestcaseRequest> testCases,
        @NotNull ProgrammingLanguage language,
        @NotNull String authorSolution
) {
}

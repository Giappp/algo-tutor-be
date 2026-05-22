package org.rap.algotutorbe.learning.dto.testcase;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SaveTestCaseRequest(@NotBlank String inputFileUrl,
                                  @NotBlank String outputFileUrl,
                                  @NotNull Integer scoreWeight,
                                  Boolean isSample,
                                  Integer sortOrder) {
}

package org.rap.algotutorbe.learning.dto.testcase;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TestCasePresignedFileRequest(
        @NotBlank String fileName,
        @NotNull TestCaseFileType fileType
) {
}
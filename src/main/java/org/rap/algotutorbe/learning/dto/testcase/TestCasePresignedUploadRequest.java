package org.rap.algotutorbe.learning.dto.testcase;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record TestCasePresignedUploadRequest(
        @NotEmpty
        List<@Valid TestCasePresignedFileRequest> files
) {
}

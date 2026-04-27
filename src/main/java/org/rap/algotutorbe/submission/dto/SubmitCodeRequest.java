package org.rap.algotutorbe.submission.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record SubmitCodeRequest(
        @NotBlank String problemSlug,
        @NotBlank String language,
        @NotBlank String code,
        List<SubmitCodeTestcaseRequest> testCases
) {
}

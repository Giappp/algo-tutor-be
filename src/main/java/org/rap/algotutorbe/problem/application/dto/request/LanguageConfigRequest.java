package org.rap.algotutorbe.problem.application.dto.request;

import jakarta.validation.constraints.NotNull;
import org.rap.algotutorbe.problem.domain.enums.ProgrammingLanguage;

public record LanguageConfigRequest(
        @NotNull ProgrammingLanguage language,
        Long timeLimitMs,
        Long memoryLimitMb,
        Long maxCodeLengthBytes,
        Long maxOutputSizeBytes,
        String codeTemplate
) {
}

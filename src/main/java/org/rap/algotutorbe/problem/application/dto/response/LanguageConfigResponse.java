package org.rap.algotutorbe.problem.application.dto.response;

import org.rap.algotutorbe.problem.domain.enums.ProgrammingLanguage;

public record LanguageConfigResponse(
        ProgrammingLanguage language,
        ConstraintsResponse constraints,
        String codeTemplate
) {
}

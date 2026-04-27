package org.rap.algotutorbe.problem.dto.response;

import org.rap.algotutorbe.problem.domain.enums.ProgrammingLanguage;

public record EditorialResponse(
        Long id,
        ProgrammingLanguage language,
        String sourceCode
) {
}

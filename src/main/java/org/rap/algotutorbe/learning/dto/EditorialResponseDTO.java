package org.rap.algotutorbe.learning.dto;

import org.rap.algotutorbe.learning.enums.ProgrammingLanguage;

public record EditorialResponseDTO(
        Long id,
        ProgrammingLanguage language,
        String sourceCode
) {
}

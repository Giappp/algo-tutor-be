package org.rap.algotutorbe.problem.dto.response;

import java.util.List;

public record RelatedProblemResponse(
        Long id,
        String title,
        String slug,
        String difficulty,
        List<String> tags
) {
}


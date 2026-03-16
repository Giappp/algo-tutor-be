package org.rap.algotutorbe.problem.application.dto.response;

public record AIContextResponse(
        String algorithmicConcept,
        String predefinedHints,
        String edgeCasesToRemind
) {
}

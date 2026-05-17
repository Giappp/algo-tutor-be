package org.rap.algotutorbe.learning.dto.landing;

import java.util.List;
import java.util.Map;

/**
 * Public response DTO for coding lesson content.
 * Matches the frontend spec: GET /lessons/:slug/coding
 */
public record CodingContentResponse(
        Long id,
        String slug,
        String title,
        String description,
        Map<String, String> starterCode,
        List<TestCaseItem> testCases,
        List<String> hints,
        Integer timeLimit,
        Integer memoryLimit
) {
    public record TestCaseItem(
            String input,
            String expectedOutput,
            Boolean isHidden
    ) {
    }
}

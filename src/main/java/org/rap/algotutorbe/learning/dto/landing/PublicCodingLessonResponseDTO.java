package org.rap.algotutorbe.learning.dto.landing;

import org.rap.algotutorbe.learning.dto.EditorialResponseDTO;
import org.rap.algotutorbe.learning.models.ProblemExample;

import java.util.List;
import java.util.Map;

/**
 * Public-safe response DTO for coding lessons.
 * Excludes sensitive data like expectedStdout from test cases.
 */
public record PublicCodingLessonResponseDTO(
        Long id,
        String title,
        String statement,
        Integer displayOrder,
        String difficulty,
        Integer baseTimeLimitMs,
        Integer baseMemoryLimitMb,
        List<String> constraints,
        Map<String, String> starterCode,
        List<String> hints,
        List<ProblemExample> examples,
        List<PublicTestCaseResponseDTO> testCases,
        List<EditorialResponseDTO> editorials
) {
}

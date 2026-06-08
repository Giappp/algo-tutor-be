package org.rap.algotutorbe.learning.dto;

import org.rap.algotutorbe.learning.dto.testcase.TestCaseDTO;
import org.rap.algotutorbe.learning.models.ProblemExample;

import java.util.List;
import java.util.Map;

public record CodingLessonResponseDTO(
        Long id,
        String title,
        String type,
        String statement,
        Integer displayOrder,
        String difficulty,
        Integer baseTimeLimitMs,
        Integer baseMemoryLimitMb,
        List<String> constraints,
        Map<String, String> starterCode,
        List<String> hints,
        List<ProblemExample> examples,
        List<TestCaseDTO> testCases,
        List<EditorialResponseDTO> editorials
) {
}

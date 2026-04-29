package org.rap.algotutorbe.learning.dto;

import org.rap.algotutorbe.learning.models.ProblemExample;

import java.util.List;
import java.util.Map;

public record CodingLessonResponseDTO(
        Long id,
        String title,
        String slug,
        String content,
        Integer orderIndex,
        Boolean isPublished,
        String difficulty,
        Integer timeLimitMs,
        Integer memoryLimitMb,
        List<String> constraints,
        Map<String, String> starterCode,
        List<String> hints,
        List<ProblemExample> examples,
        List<String> keyInsights,
        Long topicId,
        Long learningPathId,
        List<TestCaseResponseDTO> testCases,
        List<EditorialResponseDTO> editorials
) {
}

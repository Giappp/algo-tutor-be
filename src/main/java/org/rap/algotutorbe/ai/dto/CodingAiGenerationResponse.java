package org.rap.algotutorbe.ai.dto;

import org.rap.algotutorbe.learning.enums.ProgrammingLanguage;
import org.rap.algotutorbe.learning.models.ProblemExample;

import java.util.List;
import java.util.Map;

public record CodingAiGenerationResponse<T>(
        Long lessonId,
        T content,
        GenerationContext context,
        Integer inputTokens,
        Integer outputTokens
) {
    public record GenerationContext(List<Source> sources, List<Long> truncatedSourceIds) {
    }

    public record Source(Long lessonId, String title, String topicName, Boolean isPublished) {
    }

    public record ProblemContent(
            String statement,
            List<String> constraints,
            List<ProblemExample> examples,
            List<String> hints
    ) {
    }

    public record EditorialContent(
            ProgrammingLanguage language,
            String sourceCode,
            String approachSummary,
            String timeComplexity,
            String spaceComplexity
    ) {
    }

    public record StarterCodeContent(Map<String, String> starterCode, String signatureSummary) {
    }
}

package org.rap.algotutorbe.problem.application.dto.response;

import org.rap.algotutorbe.problem.domain.enums.Difficulty;

import java.util.List;
import java.util.Set;

public record ProblemDetailResponse(
        String slug,
        String title,
        String statement,
        Difficulty difficulty,
        Set<String> tags,
        LanguageConfigResponse languageConfig,
        List<TestcaseSampleResponse> sampleTestcases
) {
}
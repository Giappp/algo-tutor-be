package org.rap.algotutorbe.problem.application.dto.request;

import org.rap.algotutorbe.problem.application.dto.response.LanguageConfigResponse;

import java.util.List;

public record BenchmarkResultResponse(
        Long problemId,
        List<LanguageConfigResponse> generatedConfigs
) {
}
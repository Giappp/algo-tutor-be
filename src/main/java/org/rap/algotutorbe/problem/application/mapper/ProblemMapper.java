package org.rap.algotutorbe.problem.application.mapper;

import org.rap.algotutorbe.problem.application.dto.response.*;
import org.rap.algotutorbe.problem.domain.*;
import org.rap.algotutorbe.problem.domain.enums.ProgrammingLanguage;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ProblemMapper {
    public ProblemSummaryAdminResponse toSummaryAdmin(Problem p) {
        return new ProblemSummaryAdminResponse(
                p.getId(),
                p.getSlug(),
                p.getTitle(),
                p.getDifficulty().name(),
                p.getStatus().name(),
                p.isBenchmarked(),
                tagNames(p),
                p.getCreatedDate(),
                p.getUpdatedAt()
        );
    }

    public TestcaseAdminResponse toTestcaseAdmin(Testcase testcase) {
        return new TestcaseAdminResponse(
                testcase.getId(),
                testcase.getInput(),
                testcase.getExpectedOutput(),
                testcase.isSample(),
                testcase.getOrderIndex(),
                testcase.getExplanation()
        );
    }

    public ProblemDetailAdminResponse toDetailAdmin(Problem p) {
        return new ProblemDetailAdminResponse(
                p.getId(),
                p.getSlug(),
                p.getTitle(),
                p.getStatement(),
                p.getDifficulty().name(),
                p.getStatus().name(),
                p.isBenchmarked(),
                p.getModelSolutionCode(),
                p.getModelSolutionLanguage(),
                tagNames(p),
                p.getLanguageConfigs().stream().map(this::toLanguageConfig).toList(),
                p.getTestCases().stream().map(this::toTestcaseAdmin).toList(),
                p.getAiPromptContext() != null ? toAIContextResponse(p.getAiPromptContext()) : null,
                p.getAuthorId(),
                p.getCreatedDate(),
                p.getUpdatedAt()
        );
    }

    public LanguageConfigResponse toLanguageConfig(ProblemLanguageConfig c) {
        return toLanguageConfigResponse(c.getLanguage(), c.getConstraints(), c.getCodeTemplate());
    }

    public LanguageConfigResponse toLanguageConfigResponse(
            ProgrammingLanguage lang,
            Constraints constraints,
            String codeTemplate
    ) {
        return new LanguageConfigResponse(
                lang,
                new ConstraintsResponse(
                        constraints.getTimeLimitMs(),
                        constraints.getMemoryLimitMb(),
                        constraints.getMaxCodeLengthBytes(),
                        constraints.getMaxOutputSizeBytes()
                ),
                codeTemplate
        );
    }


    public AIContextResponse toAIContextResponse(AIPromptContext ctx) {
        if (ctx == null) return null;
        return new AIContextResponse(
                ctx.getAlgorithmicConcept(),
                ctx.getPredefinedHints(),
                ctx.getEdgeCasesToRemind()
        );
    }

    public ProblemSummaryResponse toSummary(Problem p) {
        return new ProblemSummaryResponse(
                p.getSlug(),
                p.getTitle(),
                p.getDifficulty(),
                tagNames(p)
        );
    }

    public ProblemDetailResponse toDetail(
            Problem p,
            ProblemLanguageConfig config,
            List<TestcaseSampleResponse> samples
    ) {
        return new ProblemDetailResponse(
                p.getSlug(),
                p.getTitle(),
                p.getStatement(),
                p.getDifficulty(),
                tagNames(p),
                toLanguageConfig(config),
                samples
        );
    }

    public TestcaseSampleResponse toTestcaseSample(Testcase tc) {
        return new TestcaseSampleResponse(
                tc.getId(),
                tc.getInput(),
                tc.getExpectedOutput(),
                tc.getExplanation()
        );
    }

    private Set<String> tagNames(Problem p) {
        return p.getTags().stream()
                .map(ProblemTag::getName)
                .collect(Collectors.toSet());
    }
}

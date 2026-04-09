package org.rap.algotutorbe.problem.application.mapper;

import org.rap.algotutorbe.problem.application.dto.response.*;
import org.rap.algotutorbe.problem.domain.models.AIPromptContext;
import org.rap.algotutorbe.problem.domain.models.Problem;
import org.rap.algotutorbe.problem.domain.models.Tag;
import org.rap.algotutorbe.problem.domain.models.Testcase;
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
                p.getModelSolutionCode(),
                p.getModelSolutionLanguage(),
                tagNames(p),
                p.getTestCases().stream().map(this::toTestcaseAdmin).toList(),
                p.getAiPromptContext() != null ? toAIContextResponse(p.getAiPromptContext()) : null,
                p.getAuthorId(),
                p.getCreatedDate(),
                p.getUpdatedAt()
        );
    }


    public AIContextResponse toAIContextResponse(AIPromptContext ctx) {
        if (ctx == null) return AIContextResponse.nullContext();
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
            List<TestcaseSampleResponse> samples
    ) {
        return new ProblemDetailResponse(
                p.getSlug(),
                p.getTitle(),
                p.getStatement(),
                p.getDifficulty(),
                tagNames(p),
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
                .map(Tag::getName)
                .collect(Collectors.toSet());
    }
}

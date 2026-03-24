package org.rap.algotutorbe.problem.application.services;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.rap.algotutorbe.common.api.PageResponse;
import org.rap.algotutorbe.problem.application.dto.TestcaseDto;
import org.rap.algotutorbe.problem.application.dto.response.ProblemDetailResponse;
import org.rap.algotutorbe.problem.application.dto.response.ProblemSummaryResponse;
import org.rap.algotutorbe.problem.application.exception.ProblemNotFoundException;
import org.rap.algotutorbe.problem.application.exception.TestcasesNotFoundException;
import org.rap.algotutorbe.problem.application.mapper.ProblemMapper;
import org.rap.algotutorbe.problem.domain.enums.ProblemStatus;
import org.rap.algotutorbe.problem.domain.enums.ProgrammingLanguage;
import org.rap.algotutorbe.problem.domain.models.Problem;
import org.rap.algotutorbe.problem.domain.models.ProblemLanguageConfig;
import org.rap.algotutorbe.problem.domain.models.Testcase;
import org.rap.algotutorbe.problem.domain.repositories.ProblemRepository;
import org.rap.algotutorbe.problem.domain.repositories.TestcaseRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProblemService {
    private final ProblemRepository problemRepository;
    private final TestcaseRepository testcaseRepository;
    private final ProblemMapper mapper;

    @Transactional(readOnly = true)
    public PageResponse<ProblemSummaryResponse> listPublished(Pageable pageable) {
        var problemsPage = problemRepository
                .findAllPublished(ProblemStatus.ACTIVE, pageable)
                .map(mapper::toSummary);
        return PageResponse.<ProblemSummaryResponse>builder()
                .data(problemsPage.toList())
                .currentPage(problemsPage.getNumber() + 1)
                .totalElements(problemsPage.getTotalElements())
                .totalPages(problemsPage.getTotalPages())
                .pageSize(problemsPage.getSize())
                .build();
    }

    @Transactional(readOnly = true)
    public ProblemDetailResponse getPublishedBySlug(String slug, ProgrammingLanguage language) {
        var problem = problemRepository.findPublishedBySlug(slug)
                .orElseThrow(() -> new ProblemNotFoundException(slug));

        ProblemLanguageConfig config = getProblemLanguageConfig(language, problem);

        var sampleTestcases = testcaseRepository.findSamplesByProblemId(problem.getId())
                .stream().map(mapper::toTestcaseSample).toList();

        return mapper.toDetail(problem, config, sampleTestcases);
    }

    @Transactional(readOnly = true)
    public List<TestcaseDto> getProblemTestcase(Long problemId) {
        var testcases = testcaseRepository.findByProblemIdOrderByOrderIndex(problemId)
                .orElseThrow(() -> new TestcasesNotFoundException("Testcases not found for problemId: " + problemId));
        return testcases.stream()
                .map(this::toDto)
                .toList();
    }

    private TestcaseDto toDto(Testcase testcase) {
        return new TestcaseDto(
                testcase.getInput(),
                testcase.getExpectedOutput(),
                testcase.getOrderIndex()
        );
    }

    private @NonNull ProblemLanguageConfig getProblemLanguageConfig(ProgrammingLanguage language, Problem problem) {
        return problem.getLanguageConfigs().stream()
                .filter(c -> c.getLanguage() == language)
                .findFirst()
                .or(() -> problem.getLanguageConfigs().stream()
                        .filter(c -> c.getLanguage() == ProgrammingLanguage.CPP)
                        .findFirst())
                .orElseThrow(() -> new ProblemNotFoundException(
                        "No language config found for " + language));
    }
}

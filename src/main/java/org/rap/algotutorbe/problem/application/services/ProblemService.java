package org.rap.algotutorbe.problem.application.services;

import lombok.RequiredArgsConstructor;
import org.rap.algotutorbe.common.api.PageResponse;
import org.rap.algotutorbe.problem.application.dto.response.ProblemDetailResponse;
import org.rap.algotutorbe.problem.application.dto.response.ProblemSummaryResponse;
import org.rap.algotutorbe.problem.application.exception.ProblemNotFoundException;
import org.rap.algotutorbe.problem.application.mapper.ProblemMapper;
import org.rap.algotutorbe.problem.domain.ProblemLanguageConfig;
import org.rap.algotutorbe.problem.domain.enums.ProblemStatus;
import org.rap.algotutorbe.problem.domain.enums.ProgrammingLanguage;
import org.rap.algotutorbe.problem.infrastructure.repositories.ProblemRepository;
import org.rap.algotutorbe.problem.infrastructure.repositories.TestcaseRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
                .build();
    }

    @Transactional(readOnly = true)
    public ProblemDetailResponse getPublishedBySlug(String slug, ProgrammingLanguage language) {
        var problem = problemRepository.findPublishedBySlug(slug)
                .orElseThrow(() -> new ProblemNotFoundException(slug));

        ProblemLanguageConfig config = problem.getLanguageConfigs().stream()
                .filter(c -> c.getLanguage() == language)
                .findFirst()
                .or(() -> problem.getLanguageConfigs().stream()
                        .filter(c -> c.getLanguage() == ProgrammingLanguage.CPP)
                        .findFirst())
                .orElseThrow(() -> new ProblemNotFoundException(
                        "No language config found for " + language));

        var sampleTestcases = testcaseRepository.findSamplesByProblemId(problem.getId())
                .stream().map(mapper::toTestcaseSample).toList();

        return mapper.toDetail(problem, config, sampleTestcases);
    }
}

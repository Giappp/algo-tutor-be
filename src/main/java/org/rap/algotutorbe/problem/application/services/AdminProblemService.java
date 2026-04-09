package org.rap.algotutorbe.problem.application.services;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.rap.algotutorbe.problem.application.dto.CreateProblemDto;
import org.rap.algotutorbe.problem.application.dto.TagsDto;
import org.rap.algotutorbe.problem.application.dto.request.AIContextRequest;
import org.rap.algotutorbe.problem.application.dto.request.ModelSolutionRequest;
import org.rap.algotutorbe.problem.application.dto.request.UpsertTestcasesRequest;
import org.rap.algotutorbe.problem.application.dto.response.AIContextResponse;
import org.rap.algotutorbe.problem.application.dto.response.ProblemDetailAdminResponse;
import org.rap.algotutorbe.problem.application.dto.response.ProblemSummaryAdminResponse;
import org.rap.algotutorbe.problem.application.dto.response.TestcaseAdminResponse;
import org.rap.algotutorbe.problem.application.exception.DuplicateSlugException;
import org.rap.algotutorbe.problem.application.exception.ProblemNotFoundException;
import org.rap.algotutorbe.problem.application.mapper.ProblemMapper;
import org.rap.algotutorbe.problem.domain.enums.Difficulty;
import org.rap.algotutorbe.problem.domain.enums.ProblemStatus;
import org.rap.algotutorbe.problem.domain.models.AIPromptContext;
import org.rap.algotutorbe.problem.domain.models.Problem;
import org.rap.algotutorbe.problem.domain.models.Tag;
import org.rap.algotutorbe.problem.domain.models.Testcase;
import org.rap.algotutorbe.problem.domain.repositories.ProblemRepository;
import org.rap.algotutorbe.problem.domain.repositories.TagRepository;
import org.rap.algotutorbe.problem.domain.repositories.TestcaseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)
@Slf4j
public class AdminProblemService {
    ProblemRepository problemRepository;
    TagRepository tagRepository;
    ProblemMapper problemMapper;
    TestcaseRepository testcaseRepository;

    private static @NonNull List<Testcase> getTestcases(UpsertTestcasesRequest req, Problem problem) {
        return req.testcases().stream()
                .map(r -> {
                    Testcase tc = new Testcase();
                    tc.setProblem(problem);
                    tc.setInput(r.input());
                    tc.setExpectedOutput(r.expectedOutput());
                    tc.setSample(r.isSample() != null ? r.isSample() : false);
                    tc.setOrderIndex(r.orderIndex());
                    tc.setExplanation(r.explanation());
                    return tc;
                })
                .collect(Collectors.toList());
    }

    public ProblemSummaryAdminResponse createProblem(CreateProblemDto dto, Long authorId) {
        validate(dto);
        Problem problem = mapToEntity(dto, authorId);
        Problem saved = problemRepository.save(problem);
        log.info("Created draft problem id={} slug={}", saved.getId(), saved.getSlug());
        return problemMapper.toSummaryAdmin(saved);
    }

    private Problem mapToEntity(CreateProblemDto dto, Long authorId) {
        Problem problem = new Problem();
        problem.setSlug(dto.slug());
        problem.setTitle(dto.title());
        problem.setStatement(dto.statement());
        problem.setDifficulty(Difficulty.valueOf(dto.difficulty()));
        problem.setStatus(ProblemStatus.DRAFT);
        problem.setAuthorId(authorId);

        resolveTags(dto.tags(), problem);
        return problem;
    }

    @Transactional
    public List<TestcaseAdminResponse> upsertTestcases(Long problemId, UpsertTestcasesRequest testcasesRequest) {
        Problem problem = findProblemOrThrow(problemId);
        testcaseRepository.deleteAllByProblemId(problemId);

        List<Testcase> testcases = getTestcases(testcasesRequest, problem);
        List<Testcase> saved = testcaseRepository.saveAll(testcases);
        log.info("Saved {} test cases for problem={}", saved.size(), problemId);
        return saved.stream().map(problemMapper::toTestcaseAdmin).toList();
    }

    @Transactional
    public ProblemDetailAdminResponse updateModelSolution(Long problemId, ModelSolutionRequest req) {
        Problem problem = findProblemOrThrow(problemId);

        problem.setModelSolutionCode(req.code());
        problem.setModelSolutionLanguage(req.language());

        Problem saved = problemRepository.save(problem);
        log.info("Updated model solution for problem={}", problemId);
        return problemMapper.toDetailAdmin(saved);
    }

    private void validate(CreateProblemDto dto) {
        if (problemRepository.existsBySlug(dto.slug())) {
            throw new DuplicateSlugException("Slug already exists");
        }
    }

    private void resolveTags(Set<TagsDto> tags, Problem problem) {
        if (tags == null || tags.isEmpty()) return;
        tags.forEach(dto -> {
            var tag = mapToTagEntity(dto);
            problem.addTag(tag);
        });
    }

    private Tag mapToTagEntity(TagsDto dto) {
        return tagRepository.findById(dto.id()).orElseThrow(() -> new IllegalArgumentException("Tag not found"));
    }

    private Problem findProblemOrThrow(Long id) {
        return problemRepository.findById(id)
                .orElseThrow(() -> new ProblemNotFoundException(id.toString()));
    }

    public ProblemDetailAdminResponse publishProblem(Long id) {
        Problem problem = findProblemOrThrow(id);
        //TODO: implement validation & run benchmark before release
        problem.setStatus(ProblemStatus.PUBLISHED);
        problemRepository.save(problem);
        return problemMapper.toDetailAdmin(problem);
    }

    public AIContextResponse getAIContext(Long id) {
        Problem problem = findProblemOrThrow(id);

        return problemMapper.toAIContextResponse(problem.getAiPromptContext());
    }

    public AIContextResponse updateAIContext(Long id, AIContextRequest request) {
        Problem problem = findProblemOrThrow(id);
        mapAIContext(request, problem);
        var saved = problemRepository.save(problem);
        return problemMapper.toAIContextResponse(saved.getAiPromptContext());
    }

    private void mapAIContext(AIContextRequest request, Problem problem) {
        AIPromptContext aiPromptContext = new AIPromptContext();
        aiPromptContext.setProblem(problem);
        aiPromptContext.setAlgorithmicConcept(request.algorithmicConcept());
        aiPromptContext.setPredefinedHints(request.predefinedHints());
        aiPromptContext.setEdgeCasesToRemind(request.edgeCasesToRemind());

        problem.setAiPromptContext(aiPromptContext);
    }
}

package org.rap.algotutorbe.problem.application.services;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.rap.algotutorbe.judge.SandboxClient;
import org.rap.algotutorbe.judge.SandboxRunRequest;
import org.rap.algotutorbe.judge.SandboxRunResult;
import org.rap.algotutorbe.judge.SandboxTestInput;
import org.rap.algotutorbe.problem.application.dto.CreateProblemDto;
import org.rap.algotutorbe.problem.application.dto.TagsDto;
import org.rap.algotutorbe.problem.application.dto.request.BenchmarkResultResponse;
import org.rap.algotutorbe.problem.application.dto.request.ModelSolutionRequest;
import org.rap.algotutorbe.problem.application.dto.request.UpsertTestcasesRequest;
import org.rap.algotutorbe.problem.application.dto.response.LanguageConfigResponse;
import org.rap.algotutorbe.problem.application.dto.response.ProblemDetailAdminResponse;
import org.rap.algotutorbe.problem.application.dto.response.ProblemSummaryAdminResponse;
import org.rap.algotutorbe.problem.application.dto.response.TestcaseAdminResponse;
import org.rap.algotutorbe.problem.application.exception.BenchmarkFailedException;
import org.rap.algotutorbe.problem.application.exception.DuplicateSlugException;
import org.rap.algotutorbe.problem.application.exception.ProblemNotFoundException;
import org.rap.algotutorbe.problem.application.mapper.ProblemMapper;
import org.rap.algotutorbe.problem.domain.*;
import org.rap.algotutorbe.problem.domain.enums.Difficulty;
import org.rap.algotutorbe.problem.domain.enums.ProblemStatus;
import org.rap.algotutorbe.problem.domain.enums.ProgrammingLanguage;
import org.rap.algotutorbe.problem.infrastructure.repositories.ProblemLangConfigRepository;
import org.rap.algotutorbe.problem.infrastructure.repositories.ProblemRepository;
import org.rap.algotutorbe.problem.infrastructure.repositories.TagRepository;
import org.rap.algotutorbe.problem.infrastructure.repositories.TestcaseRepository;
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
    ProblemLangConfigRepository problemLangConfigRepository;
    SandboxClient sandboxClient;

    @Transactional
    public List<TestcaseAdminResponse> upsertTestcases(Long problemId, UpsertTestcasesRequest req) {
        Problem problem = findProblemOrThrow(problemId);

        // Replacing test cases invalidates current benchmark
        invalidateBenchmark(problem);
        testcaseRepository.deleteAllByProblemId(problemId);

        List<Testcase> testcases = req.testcases().stream()
                .map(r -> {
                    Testcase tc = new Testcase();
                    tc.setProblem(problem);
                    tc.setInput(r.input());
                    tc.setExpectedOutput(r.expectedOutput());
                    tc.setSample(r.isSample());
                    tc.setOrderIndex(r.orderIndex());
                    tc.setExplanation(r.explanation());
                    return tc;
                })
                .collect(Collectors.toList());

        List<Testcase> saved = testcaseRepository.saveAll(testcases);
        log.info("Saved {} test cases for problem={}", saved.size(), problemId);
        return saved.stream().map(problemMapper::toTestcaseAdmin).toList();
    }

    public ProblemSummaryAdminResponse createProblem(CreateProblemDto dto, Long authorId) {
        validate(dto);
        Problem problem = mapToEntity(dto, authorId);
        Problem saved = problemRepository.save(problem);
        log.info("Created draft problem id={} slug={}", saved.getId(), saved.getSlug());
        return problemMapper.toSummaryAdmin(saved);
    }

    @Transactional
    public ProblemDetailAdminResponse updateModelSolution(Long problemId, ModelSolutionRequest req) {
        Problem problem = findProblemOrThrow(problemId);

        // Changing the model solution invalidates current benchmark
        invalidateBenchmark(problem);
        problem.setModelSolutionCode(req.code());
        problem.setModelSolutionLanguage(req.language());

        Problem saved = problemRepository.save(problem);
        log.info("Updated model solution for problem={}", problemId);
        return problemMapper.toDetailAdmin(saved);
    }

    @Transactional
    public BenchmarkResultResponse runBenchmark(Long problemId) {
        Problem problem = findProblemOrThrow(problemId);

        List<Testcase> testcases = testcaseRepository.findByProblemIdOrderByOrderIndex(problemId);
        if (testcases.isEmpty()) {
            throw new BenchmarkFailedException("No test cases found. Upload test cases first.");
        }
        if (problem.getModelSolutionCode() == null) {
            throw new BenchmarkFailedException("No model solution found. Upload a model solution first.");
        }

        // Build sandbox request using a generous default limit for the benchmark run
        Constraints benchmarkLimit = Constraints.defaults();
        List<SandboxTestInput> inputs = testcases.stream()
                .map(tc -> new SandboxTestInput(tc.getInput(), tc.getExpectedOutput()))
                .toList();

        SandboxRunRequest sandboxReq = new SandboxRunRequest(
                problem.getModelSolutionCode(),
                problem.getModelSolutionLanguage().name(),
                inputs,
                benchmarkLimit.getTimeLimitMs() * 5,   // generous budget for profiling
                benchmarkLimit.getMemoryLimitMb() * 2
        );

        SandboxRunResult result = sandboxClient.run(sandboxReq);
        if (!result.allPassed()) {
            throw new BenchmarkFailedException("Model solution did not pass all test cases.");
        }

        // Derive base constraints from observed peak values (add 50% headroom)
        Constraints base = new Constraints(
                Math.round(result.maxObservedTimeMs() * 1.5),
                result.maxObservedMemoryMb() + 32,
                Constraints.defaults().getMaxCodeLengthBytes(),
                Constraints.defaults().getMaxOutputSizeBytes()
        );

        // Clear stale configs and regenerate for every supported language
        problemLangConfigRepository.deleteAllByProblemId(problemId);
        problem.getLanguageConfigs().clear();

        List<LanguageConfigResponse> generated = new java.util.ArrayList<>();
        for (ProgrammingLanguage lang : ProgrammingLanguage.values()) {
            Constraints langConstraints = lang.calculateConstraints(base);
            ProblemLanguageConfig config = new ProblemLanguageConfig();
            config.setLanguage(lang);
            config.setConstraints(langConstraints);
            problem.addLanguageConfig(config);
            generated.add(problemMapper.toLanguageConfigResponse(lang, langConstraints, null));
        }

        problem.markAsBenchmarked();
        problemRepository.save(problem);
        log.info("Benchmark complete for problem={}. Configs generated for {} languages.",
                problemId, generated.size());

        return new BenchmarkResultResponse(problemId, generated);
    }

    private Problem mapToEntity(CreateProblemDto dto, Long authorId) {
        Problem problem = new Problem();
        problem.setSlug(dto.slug());
        problem.setTitle(dto.title());
        problem.setStatement(dto.statement());
        problem.setDifficulty(Difficulty.valueOf(dto.difficulty()));
        problem.setStatus(ProblemStatus.DRAFT);
        problem.setAuthorId(authorId);
        problem.setBenchmarked(false);

        resolveTags(dto.tags(), problem);
        return problem;
    }

    private ProblemTag mapToTagEntity(TagsDto dto) {
        return tagRepository.findById(dto.id()).orElseThrow(() -> new IllegalArgumentException("Tag not found"));
    }

    private void validate(CreateProblemDto dto) {
        if (problemRepository.existsBySlug(dto.slug())) {
            throw new DuplicateSlugException("Slug already exists");
        }
        if (dto.title() == null || dto.title().isBlank()) {
            throw new IllegalArgumentException("Title is required");
        }
        if (dto.difficulty() == null || dto.difficulty().isBlank()) {
            throw new IllegalArgumentException("Difficulty is required");
        }
        if (dto.tags() == null || dto.tags().isEmpty()) {
            throw new IllegalArgumentException("Tags is required");
        }
    }

    private void resolveTags(Set<TagsDto> tagNames, Problem problem) {
        if (tagNames == null || tagNames.isEmpty()) return;
        tagNames.forEach(dto -> {
            var tag = mapToTagEntity(dto);
            problem.addTag(tag);
        });
    }

    private Problem findProblemOrThrow(Long id) {
        return problemRepository.findById(id)
                .orElseThrow(() -> new ProblemNotFoundException(id.toString()));
    }

    private void invalidateBenchmark(Problem problem) {
        if (problem.isBenchmarked()) {
            log.warn("Invalidating benchmark for problem={}", problem.getId());
            problem.setBenchmarked(false);
            problemLangConfigRepository.deleteAllByProblemId(problem.getId());
            problem.getLanguageConfigs().clear();
        }
    }
}

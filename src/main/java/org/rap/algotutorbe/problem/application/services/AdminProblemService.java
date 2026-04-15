package org.rap.algotutorbe.problem.application.services;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.rap.algotutorbe.common.api.PageResponse;
import org.rap.algotutorbe.common.errors.ErrorCode;
import org.rap.algotutorbe.common.exception.AppException;
import org.rap.algotutorbe.common.services.BaseService;
import org.rap.algotutorbe.judge.PistonValidationService;
import org.rap.algotutorbe.judge.dto.ValidationResult;
import org.rap.algotutorbe.judge.exception.SolutionValidationException;
import org.rap.algotutorbe.problem.application.dto.CreateProblemDto;
import org.rap.algotutorbe.problem.application.dto.TagDto;
import org.rap.algotutorbe.problem.application.dto.request.*;
import org.rap.algotutorbe.problem.application.dto.response.AIContextResponse;
import org.rap.algotutorbe.problem.application.dto.response.ProblemDetailAdminResponse;
import org.rap.algotutorbe.problem.application.dto.response.ProblemSummaryAdminResponse;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)
@Slf4j
public class AdminProblemService extends BaseService {
    ProblemRepository problemRepository;
    TagRepository tagRepository;
    ProblemMapper problemMapper;
    TestcaseRepository testcaseRepository;
    PistonValidationService pistonValidationService;
    TestcasePersistenceService testcasePersistenceService;

    public ProblemSummaryAdminResponse createProblem(CreateProblemDto dto) {
        validate(dto);
        Problem problem = mapToEntity(dto);
        Problem saved = problemRepository.save(problem);
        log.info("Created draft problem id={} slug={}", saved.getId(), saved.getSlug());
        return problemMapper.toSummaryAdmin(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProblemSummaryAdminResponse> listProblems(Pageable pageable) {
        Page<ProblemSummaryAdminResponse> page = problemRepository.findAllForAdmin(pageable)
                .map(problemMapper::toSummaryAdmin);
        return PageResponse.<ProblemSummaryAdminResponse>builder()
                .data(page.toList())
                .currentPage(page.getNumber() + 1)
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .pageSize(page.getSize())
                .build();
    }

    @Transactional(readOnly = true)
    public ProblemDetailAdminResponse getProblemDetail(Long id) {
        Problem problem = findProblemOrThrow(id);
        return problemMapper.toDetailAdmin(problem);
    }

    @Transactional
    public ProblemDetailAdminResponse updateProblem(Long id, UpdateProblemRequest request) {
        Problem problem = findProblemOrThrow(id);

        problem.setTitle(request.title());
        problem.setStatement(request.statement());
        problem.setDifficulty(Difficulty.valueOf(request.difficulty()));

        // Replace tags entirely
        problem.getTags().clear();
        resolveTags(request.tags(), problem);

        Problem saved = problemRepository.save(problem);
        log.info("Updated problem id={}", saved.getId());
        return problemMapper.toDetailAdmin(saved);
    }

    @Transactional
    public void archiveProblem(Long id) {
        Problem problem = findProblemOrThrow(id);
        problem.setStatus(ProblemStatus.ARCHIVED);
        problemRepository.save(problem);
        log.info("Archived problem id={}", id);
    }

    @Transactional
    public void unarchiveProblem(Long id) {
        Problem problem = findProblemOrThrow(id);
        problem.setStatus(ProblemStatus.DRAFT);
        problemRepository.save(problem);
        log.info("Unarchived problem id={}", id);
    }

    public void upsertTestcases(Long problemId, RunTestcasesRequest request) {
        Problem problem = findProblemOrThrow(problemId);

        // 1. Chạy Piston validate (Không đặt @Transactional ở đây để tránh treo DB Connection)
        ValidationResult validationResult = pistonValidationService.validateSolution(
                request.language(),
                request.authorSolution(),
                request.testCases()
        );

        if (!validationResult.isAllPassed()) {
            throw new SolutionValidationException("Lời giải mẫu không vượt qua testcase", validationResult.getDetails());
        }

        // 2. Pass thì mới lưu xuống DB
        testcasePersistenceService.saveValidatedTestcasesTransactionally(problem, request);
    }

    /**
     * API 2: Bổ sung thêm Editorial cho ngôn ngữ khác (hoặc sửa code của ngôn ngữ hiện tại)
     */
    public ProblemDetailAdminResponse updateModelSolution(Long problemId, ModelSolutionRequest req) {
        Problem problem = findProblemOrThrow(problemId);

        List<Testcase> existingTestcases = testcaseRepository.findByProblemId(problemId);
        if (existingTestcases.isEmpty()) {
            throw new AppException(ErrorCode.EDITORIAL_TESTCASE_MISSING);
        }

        // Map Entity sang DTO để ném vào Piston
        List<TestcaseRequest> testcaseRequests = existingTestcases.stream()
                .map(tc -> new TestcaseRequest(
                        tc.getId(),
                        tc.getInput(),
                        tc.getExpectedOutput(),
                        tc.isSample(),
                        tc.getOrderIndex(),
                        tc.getExplanation()))
                .toList();

        // 2. Gọi Piston chấm code mới với testcase cũ
        ValidationResult validationResult = pistonValidationService.validateSolution(
                req.language(),
                req.code(),
                testcaseRequests
        );

        if (!validationResult.isAllPassed()) {
            throw new SolutionValidationException("Lời giải bổ sung không vượt qua bộ testcase hiện tại", validationResult.getDetails());
        }

        // 3. Nếu Pass, lưu Editorial an toàn
        testcasePersistenceService.saveEditorialTransactionally(problem, req.language(), req.code());
        return problemMapper.toDetailAdmin(problem);
    }

    // ====================================================================================
    // CÁC HÀM TIỆN ÍCH
    // ====================================================================================

    private void validate(CreateProblemDto dto) {
        if (problemRepository.existsBySlug(dto.slug())) {
            throw new DuplicateSlugException("Slug already exists");
        }
    }

    private Problem mapToEntity(CreateProblemDto dto) {
        Long authorId = getCurrentUserIdOrThrow();
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

    private void resolveTags(Set<TagDto> tags, Problem problem) {
        if (tags == null || tags.isEmpty()) return;
        tags.forEach(dto -> {
            var tag = mapToTagEntity(dto);
            problem.addTag(tag);
        });
    }

    private Tag mapToTagEntity(TagDto dto) {
        return tagRepository.findById(dto.id()).orElseThrow(() -> new IllegalArgumentException("Tag not found"));
    }

    private Problem findProblemOrThrow(Long id) {
        return problemRepository.findById(id)
                .orElseThrow(() -> new ProblemNotFoundException(id.toString()));
    }

    public ProblemDetailAdminResponse publishProblem(Long id) {
        Problem problem = findProblemOrThrow(id);
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
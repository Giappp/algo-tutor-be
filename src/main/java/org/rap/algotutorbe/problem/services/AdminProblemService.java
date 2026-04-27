package org.rap.algotutorbe.problem.services;

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
import org.rap.algotutorbe.problem.domain.models.Editorial;
import org.rap.algotutorbe.problem.domain.models.Problem;
import org.rap.algotutorbe.problem.domain.models.Tag;
import org.rap.algotutorbe.problem.domain.models.Testcase;
import org.rap.algotutorbe.problem.dto.request.*;
import org.rap.algotutorbe.problem.dto.response.EditorialResponse;
import org.rap.algotutorbe.problem.dto.response.ProblemDetailAdminResponse;
import org.rap.algotutorbe.problem.dto.response.ProblemSummaryAdminResponse;
import org.rap.algotutorbe.problem.dto.response.testcase.FailedItem;
import org.rap.algotutorbe.problem.dto.response.testcase.SuccessItem;
import org.rap.algotutorbe.problem.dto.response.testcase.SummaryInfo;
import org.rap.algotutorbe.problem.dto.response.testcase.TestcasesResponse;
import org.rap.algotutorbe.problem.exception.ProblemNotFoundException;
import org.rap.algotutorbe.problem.mapper.ProblemMapper;
import org.rap.algotutorbe.problem.repositories.EditorialRepository;
import org.rap.algotutorbe.problem.repositories.ProblemRepository;
import org.rap.algotutorbe.problem.repositories.TagRepository;
import org.rap.algotutorbe.problem.repositories.TestcaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)
@Slf4j
public class AdminProblemService extends BaseService {
    ProblemRepository problemRepository;
    TagRepository tagRepository;
    EditorialRepository editorialRepository;
    ProblemMapper problemMapper;
    TestcaseRepository testcaseRepository;
    PistonValidationService pistonValidationService;
    TestcasePersistenceService testcasePersistenceService;

    public ProblemSummaryAdminResponse createProblem(CreateProblemAdminRequest request) {
        Problem problem = problemMapper.toEntity(request);

        if (request.tags() != null && !request.tags().isEmpty()) {
            List<Tag> tags = tagRepository.findAllById(request.tags());
            if (tags.size() != request.tags().size()) {
                throw new AppException(ErrorCode.INVALID_PAYLOAD);
            }
            tags.forEach(problem::addTag);
        }
        Problem saved = problemRepository.save(problem);
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
    public ProblemDetailAdminResponse updateProblem(Long id, UpdateProblemAdminRequest request) {
        Problem problem = findProblemOrThrow(id);

        problemMapper.updateEntity(request, problem);

        if (request.tags() != null) {
            problem.getTags().clear();
            resolveTagsByName(request.tags(), problem);
        }

        Problem saved = problemRepository.save(problem);
        log.info("Updated problem id={}", saved.getId());
        return problemMapper.toDetailAdmin(saved);
    }

    @Transactional
    public void archiveProblem(Long id) {
        Problem problem = findProblemOrThrow(id);
        problemRepository.save(problem);
        log.info("Archived problem id={}", id);
    }

    @Transactional
    public void unarchiveProblem(Long id) {
        Problem problem = findProblemOrThrow(id);
        problemRepository.save(problem);
        log.info("Unarchived problem id={}", id);
    }

    public TestcasesResponse insertTestcases(Long problemId, TestcasesRequest request) {
        Problem problem = findProblemOrThrow(problemId);

        int totalReceived = request.testCases().size();
        List<SuccessItem> successItems = new java.util.ArrayList<>();
        List<FailedItem> failedItems = new java.util.ArrayList<>();

        for (int i = 0; i < request.testCases().size(); i++) {
            TestcaseRequest tc = request.testCases().get(i);
            try {
                Testcase entity = new Testcase();
                entity.setProblem(problem);
                entity.setStdin(tc.stdin());
                entity.setExpectedStdout(tc.expectedStdout());
                entity.setHidden(tc.isSample() == null || !tc.isSample());
                entity.setOrderIndex(tc.orderIndex() != null ? tc.orderIndex() : i);
                entity.setExplanation(tc.explanation());

                Testcase saved = testcaseRepository.save(entity);
                successItems.add(new SuccessItem(saved.getId(), saved.getOrderIndex()));
            } catch (Exception e) {
                log.warn("Failed to save testcase {} for problem {}: {}", i, problemId, e.getMessage());
                failedItems.add(new FailedItem(i, tc.id(), e.getMessage()));
            }
        }

        int successCount = successItems.size();
        int failedCount = failedItems.size();
        SummaryInfo summary = new SummaryInfo(totalReceived, successCount, failedCount);

        log.info("Inserted testcases for problem {}: {} success, {} failed", problemId, successCount, failedCount);
        return new TestcasesResponse(problemId, summary, successItems, failedItems);
    }

    public ProblemDetailAdminResponse updateModelSolution(Long problemId, ModelSolutionRequest req) {
        Problem problem = findProblemOrThrow(problemId);

        List<Testcase> existingTestcases = testcaseRepository.findByProblemId(problemId);
        if (existingTestcases.isEmpty()) {
            throw new AppException(ErrorCode.EDITORIAL_TESTCASE_MISSING);
        }

        ValidationResult validationResult = pistonValidationService.validateSolution(
                req.language(),
                req.code(),
                existingTestcases
        );

        if (!validationResult.isAllPassed()) {
            throw new SolutionValidationException("Loi giai bo sung khong vuot qua bo testcase hien tai", validationResult.details());
        }

        testcasePersistenceService.saveEditorialTransactionally(problem, req.language(), req.code());
        return problemMapper.toDetailAdmin(problem);
    }

    // ====================================================================================
    // EDITORIAL CRUD
    // ====================================================================================

    @Transactional
    public EditorialResponse createEditorial(Long problemId, CreateEditorialRequest request) {
        Problem problem = findProblemOrThrow(problemId);

        Editorial editorial = new Editorial();
        editorial.setProblem(problem);
        editorial.setLanguage(request.language());
        editorial.setSourceCode(request.sourceCode());

        Editorial saved = editorialRepository.save(editorial);
        log.info("Created editorial id={} for problem={} language={}", saved.getId(), problemId, request.language());
        return problemMapper.toEditorialResponse(saved);
    }

    @Transactional
    public EditorialResponse updateEditorial(Long problemId, Long editorialId, UpdateEditorialRequest request) {
        Editorial editorial = findEditorialOrThrow(editorialId);
        validateEditorialBelongsToProblem(editorial, problemId);

        editorial.setSourceCode(request.sourceCode());
        Editorial saved = editorialRepository.save(editorial);
        log.info("Updated editorial id={}", saved.getId());
        return problemMapper.toEditorialResponse(saved);
    }

    @Transactional
    public void deleteEditorial(Long problemId, Long editorialId) {
        Editorial editorial = findEditorialOrThrow(editorialId);
        validateEditorialBelongsToProblem(editorial, problemId);
        editorialRepository.delete(editorial);
        log.info("Deleted editorial id={}", editorialId);
    }

    @Transactional(readOnly = true)
    public List<EditorialResponse> getEditorials(Long problemId) {
        findProblemOrThrow(problemId);
        return editorialRepository.findAll().stream()
                .filter(e -> e.getProblem() != null && e.getProblem().getId().equals(problemId))
                .map(problemMapper::toEditorialResponse)
                .toList();
    }

    private void resolveTagsByName(List<String> tagNames, Problem problem) {
        if (tagNames == null || tagNames.isEmpty()) return;
        tagNames.stream()
                .filter(name -> name != null && !name.isBlank())
                .distinct()
                .forEach(name -> {
                    Tag tag = tagRepository.findByNameIgnoreCase(name.trim())
                            .orElseGet(() -> {
                                Tag newTag = new Tag();
                                newTag.setName(name.trim());
                                newTag.setSlug(name.trim().toLowerCase().replaceAll("\\s+", "-"));
                                return tagRepository.save(newTag);
                            });
                    problem.addTag(tag);
                });
    }

    private Editorial findEditorialOrThrow(Long id) {
        return editorialRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
    }

    private void validateEditorialBelongsToProblem(Editorial editorial, Long problemId) {
        if (editorial.getProblem() == null || !editorial.getProblem().getId().equals(problemId)) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }
    }

    private Problem findProblemOrThrow(Long id) {
        return problemRepository.findById(id)
                .orElseThrow(() -> new ProblemNotFoundException(id.toString()));
    }
}
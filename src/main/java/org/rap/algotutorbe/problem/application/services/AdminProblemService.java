package org.rap.algotutorbe.problem.application.services;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.rap.algotutorbe.common.services.BaseService;
import org.rap.algotutorbe.judge.PistonValidationService;
import org.rap.algotutorbe.judge.dto.ValidationResult;
import org.rap.algotutorbe.judge.exception.SolutionValidationException;
import org.rap.algotutorbe.problem.application.dto.CreateProblemDto;
import org.rap.algotutorbe.problem.application.dto.TagsDto;
import org.rap.algotutorbe.problem.application.dto.request.AIContextRequest;
import org.rap.algotutorbe.problem.application.dto.request.ModelSolutionRequest;
import org.rap.algotutorbe.problem.application.dto.request.RunTestcasesRequest;
import org.rap.algotutorbe.problem.application.dto.request.TestcaseRequest;
import org.rap.algotutorbe.problem.application.dto.response.AIContextResponse;
import org.rap.algotutorbe.problem.application.dto.response.ProblemDetailAdminResponse;
import org.rap.algotutorbe.problem.application.dto.response.ProblemSummaryAdminResponse;
import org.rap.algotutorbe.problem.application.exception.DuplicateSlugException;
import org.rap.algotutorbe.problem.application.exception.ProblemNotFoundException;
import org.rap.algotutorbe.problem.application.mapper.ProblemMapper;
import org.rap.algotutorbe.problem.domain.enums.Difficulty;
import org.rap.algotutorbe.problem.domain.enums.ProblemStatus;
import org.rap.algotutorbe.problem.domain.enums.ProgrammingLanguage;
import org.rap.algotutorbe.problem.domain.models.*;
import org.rap.algotutorbe.problem.domain.repositories.EditorialRepository;
import org.rap.algotutorbe.problem.domain.repositories.ProblemRepository;
import org.rap.algotutorbe.problem.domain.repositories.TagRepository;
import org.rap.algotutorbe.problem.domain.repositories.TestcaseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
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
    EditorialRepository editorialRepository;

    public ProblemSummaryAdminResponse createProblem(CreateProblemDto dto) {
        validate(dto);
        Problem problem = mapToEntity(dto);
        Problem saved = problemRepository.save(problem);
        log.info("Created draft problem id={} slug={}", saved.getId(), saved.getSlug());
        return problemMapper.toSummaryAdmin(saved);
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
        saveValidatedTestcasesTransactionally(problem, request);
    }

    @Transactional
    protected void saveValidatedTestcasesTransactionally(Problem problem, RunTestcasesRequest request) {
        // Chỉ xóa testcase cũ, KHÔNG xóa các Editorials (để bảo toàn ngôn ngữ khác)
        testcaseRepository.deleteAllByProblemId(problem.getId());

        // Map và lưu list testcases mới
        List<Testcase> testcaseEntities = request.testCases().stream()
                .map(tc -> new Testcase(problem, tc.input(), tc.expectedOutput(), tc.isSample(), tc.orderIndex(), tc.explanation()))
                .toList();
        testcaseRepository.saveAll(testcaseEntities);

        // Upsert lời giải chuẩn (Validator Solution)
        upsertEditorialSafe(problem, request.language(), request.authorSolution());

        // Bật cờ PUBLISHED nếu đang là DRAFT
        if (problem.getStatus() == ProblemStatus.DRAFT) {
            problem.setStatus(ProblemStatus.PUBLISHED);
        }
        problemRepository.save(problem);
        log.info("Upserted testcases and validator solution for problem={}", problem.getId());
    }

    /**
     * API 2: Bổ sung thêm Editorial cho ngôn ngữ khác (hoặc sửa code của ngôn ngữ hiện tại)
     */
    public ProblemDetailAdminResponse updateModelSolution(Long problemId, ModelSolutionRequest req) {
        Problem problem = findProblemOrThrow(problemId);

        List<Testcase> existingTestcases = testcaseRepository.findByProblemId(problemId);
        if (existingTestcases.isEmpty()) {
            throw new IllegalStateException("Không thể thêm lời giải phụ khi bài tập chưa có testcase.");
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
        saveEditorialTransactionally(problem, req.language(), req.code());
        return problemMapper.toDetailAdmin(problem);
    }

    @Transactional
    protected void saveEditorialTransactionally(Problem problem, ProgrammingLanguage language, String code) {
        upsertEditorialSafe(problem, language, code);
        problemRepository.save(problem);
        log.info("Upserted additional solution ({}) for problem={}", language, problem.getId());
    }

    /**
     * Hàm Helper quan trọng: Update code nếu ngôn ngữ đã tồn tại, hoặc thêm mới nếu chưa có.
     * Ngăn chặn việc sinh ra 2 lời giải Java trùng lặp.
     */
    private void upsertEditorialSafe(Problem problem, ProgrammingLanguage language, String code) {
        Optional<Editorial> existingEditorial = problem.getEditorials().stream()
                .filter(ed -> ed.getLanguage().equals(language))
                .findFirst();

        if (existingEditorial.isPresent()) {
            // Update code nếu đã có
            existingEditorial.get().setSourceCode(code);
        } else {
            // Tạo mới nếu chưa có
            Editorial newEditorial = new Editorial(problem, language, code);
            problem.addEditorial(newEditorial);
        }
    }

    // ====================================================================================
    // CÁC HÀM TIỆN ÍCH KHÁC (GIỮ NGUYÊN)
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
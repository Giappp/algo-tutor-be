package org.rap.algotutorbe.problem.application.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rap.algotutorbe.problem.application.dto.request.RunTestcasesRequest;
import org.rap.algotutorbe.problem.domain.enums.ProblemStatus;
import org.rap.algotutorbe.problem.domain.enums.ProgrammingLanguage;
import org.rap.algotutorbe.problem.domain.models.Editorial;
import org.rap.algotutorbe.problem.domain.models.Problem;
import org.rap.algotutorbe.problem.domain.models.Testcase;
import org.rap.algotutorbe.problem.domain.repositories.ProblemRepository;
import org.rap.algotutorbe.problem.domain.repositories.TestcaseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TestcasePersistenceService {
    private final TestcaseRepository testcaseRepository;
    private final ProblemRepository problemRepository;

    @Transactional
    protected void saveValidatedTestcasesTransactionally(Problem problem, RunTestcasesRequest request) {
        // Chỉ xóa testcase cũ, KHÔNG xóa các Editorials (để bảo toàn ngôn ngữ khác)
        testcaseRepository.deleteAllByProblemId(problem.getId());

        // Map và lưu list testcases mới
        List<Testcase> testcaseEntities = request.testCases().stream()
                .map(tc -> new Testcase(problem, tc.input(), tc.expectedOutput(), tc.isSample(), tc.orderIndex(), tc.explanation()))
                .toList();
        testcaseRepository.saveAll(testcaseEntities);

        upsertEditorialSafe(problem, request.language(), request.authorSolution());

        // Bật cờ PUBLISHED nếu đang là DRAFT
        if (problem.getStatus() == ProblemStatus.DRAFT) {
            problem.setStatus(ProblemStatus.PUBLISHED);
        }
        problemRepository.save(problem);
        log.info("Upserted testcases and validator solution for problem={}", problem.getId());
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

    @Transactional
    protected void saveEditorialTransactionally(Problem problem, ProgrammingLanguage language, String code) {
        upsertEditorialSafe(problem, language, code);
        problemRepository.save(problem);
        log.info("Upserted additional solution ({}) for problem={}", language, problem.getId());
    }
}

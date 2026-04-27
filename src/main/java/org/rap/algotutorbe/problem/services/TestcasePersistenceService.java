package org.rap.algotutorbe.problem.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rap.algotutorbe.problem.domain.enums.ProgrammingLanguage;
import org.rap.algotutorbe.problem.domain.models.Editorial;
import org.rap.algotutorbe.problem.domain.models.Problem;
import org.rap.algotutorbe.problem.domain.models.Testcase;
import org.rap.algotutorbe.problem.dto.request.TestcasesRequest;
import org.rap.algotutorbe.problem.repositories.ProblemRepository;
import org.rap.algotutorbe.problem.repositories.TestcaseRepository;
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
    public void saveValidatedTestcasesTransactionally(Problem problem, TestcasesRequest request) {
        testcaseRepository.deleteAllByProblemId(problem.getId());

        List<Testcase> testcaseEntities = request.testCases().stream()
                .map(tc -> {
                    Testcase t = new Testcase();
                    t.setProblem(problem);
                    t.setStdin(tc.stdin());
                    t.setExpectedStdout(tc.expectedStdout());
                    boolean isSample = tc.isSample() != null && tc.isSample();
                    t.setHidden(!isSample);
                    t.setOrderIndex(tc.orderIndex() != null ? tc.orderIndex() : 0);
                    t.setExplanation(tc.explanation());
                    return t;
                })
                .toList();
        testcaseRepository.saveAll(testcaseEntities);

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
            Editorial newEditorial = new Editorial();
            newEditorial.setProblem(problem);
            newEditorial.setLanguage(language);
            newEditorial.setSourceCode(code);
            problem.addEditorial(newEditorial);
        }
    }

    @Transactional
    public void saveEditorialTransactionally(Problem problem, ProgrammingLanguage language, String code) {
        upsertEditorialSafe(problem, language, code);
        problemRepository.save(problem);
        log.info("Upserted additional solution ({}) for problem={}", language, problem.getId());
    }
}

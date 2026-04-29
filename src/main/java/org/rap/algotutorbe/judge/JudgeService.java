package org.rap.algotutorbe.judge;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rap.algotutorbe.judge.dto.JudgeResult;
import org.rap.algotutorbe.judge.dto.TestcaseJudgeResult;
import org.rap.algotutorbe.learning.enums.ProgrammingLanguage;
import org.rap.algotutorbe.learning.models.CodingLesson;
import org.rap.algotutorbe.learning.models.Testcase;
import org.rap.algotutorbe.learning.repositories.CodingLessonRepository;
import org.rap.algotutorbe.submission.dto.SubmissionCreatedMessage;
import org.rap.algotutorbe.submission.entities.Verdict;
import org.rap.algotutorbe.submission.service.SubmissionService;
import org.rap.algotutorbe.submission.service.SubmissionTestcaseService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class JudgeService {
    private static final int DEFAULT_COMPILE_TIMEOUT_MS = 10_000;

    private final SubmissionService submissionService;
    private final SubmissionTestcaseService submissionTestcaseService;
    private final CodingLessonRepository codingLessonRepository;
    private final PistonClient pistonClient;

    public JudgeResult processSubmission(SubmissionCreatedMessage message) {
        UUID submissionId = message.submissionId();
        log.info("Processing submission: submissionId={}, codingLessonId={}, language={}",
                submissionId, message.codingLessonId(), message.programmingLanguage());

        submissionService.markAsProcessing(submissionId);

        CodingLesson lesson = loadLessonWithTestcases(message.codingLessonId());
        List<TestcaseJudgeResult> results = judgeAllTestcases(lesson, message);
        Verdict finalVerdict = computeFinalVerdict(results, lesson.getTestCases().size());
        int passedCount = countPassed(results);

        JudgeResult judgeResult = new JudgeResult(
                finalVerdict,
                passedCount,
                lesson.getTestCases().size(),
                maxCpuTime(results),
                maxMemory(results),
                extractCompilationOutput(results),
                results
        );

        persistResults(judgeResult, submissionId, finalVerdict, passedCount, results);

        log.info("Submission [{}] judged: verdict={}, passed={}/{}",
                submissionId, finalVerdict, passedCount, lesson.getTestCases().size());

        return judgeResult;
    }

    // ── Private helpers ────────────────────────────────────────────

    private CodingLesson loadLessonWithTestcases(Long lessonId) {
        return codingLessonRepository.findByIdWithTestCases(lessonId)
                .orElseThrow(() -> new IllegalStateException("CodingLesson not found: " + lessonId));
    }

    private List<TestcaseJudgeResult> judgeAllTestcases(CodingLesson lesson, SubmissionCreatedMessage message) {
        List<Testcase> testcases = lesson.getTestCases();
        if (testcases == null || testcases.isEmpty()) {
            throw new IllegalStateException("No testcases found for coding lesson: " + lesson.getId());
        }

        ProgrammingLanguage language = message.programmingLanguage();
        int baseTimeLimitMs = lesson.getBaseTimeLimitMs();
        int baseMemoryLimitMb = lesson.getBaseMemoryLimitMb();
        String sourceCode = message.sourceCode();

        List<TestcaseJudgeResult> results = new ArrayList<>();

        for (int i = 0; i < testcases.size(); i++) {
            Testcase tc = testcases.get(i);
            int index = tc.getOrderIndex() >= 0 ? tc.getOrderIndex() : i;

            int runTimeoutMs = language.calculatePistonRunTimeout(baseTimeLimitMs);
            int memoryLimitMb = language.calculateMemoryLimit(baseMemoryLimitMb);

            TestcaseJudgeResult result = pistonClient.executeForJudging(
                    index, language, sourceCode, tc,
                    runTimeoutMs, DEFAULT_COMPILE_TIMEOUT_MS, memoryLimitMb
            );

            results.add(result);

            if (result.verdict() == Verdict.COMPILATION_ERROR) {
                log.warn("Submission [{}]: COMPILATION_ERROR on testcase {}", message.submissionId(), index);
                break;
            }
        }

        return results;
    }

    private Verdict computeFinalVerdict(List<TestcaseJudgeResult> results, int totalCount) {
        if (results.stream().anyMatch(r -> r.verdict() == Verdict.COMPILATION_ERROR)) {
            return Verdict.COMPILATION_ERROR;
        }
        if (results.size() == totalCount && results.stream().allMatch(r -> r.verdict() == Verdict.ACCEPTED)) {
            return Verdict.ACCEPTED;
        }
        return results.stream()
                .map(TestcaseJudgeResult::verdict)
                .filter(v -> v != Verdict.PENDING && v != Verdict.PROCESSING && v != Verdict.ACCEPTED)
                .findFirst()
                .orElse(Verdict.ACCEPTED);
    }

    private int countPassed(List<TestcaseJudgeResult> results) {
        return (int) results.stream()
                .filter(r -> r.verdict() == Verdict.ACCEPTED)
                .count();
    }

    private int maxCpuTime(List<TestcaseJudgeResult> results) {
        return results.stream()
                .mapToInt(r -> r.cpuTime() != null ? r.cpuTime() : 0)
                .max()
                .orElse(0);
    }

    private int maxMemory(List<TestcaseJudgeResult> results) {
        return results.stream()
                .mapToInt(r -> r.memory() != null ? r.memory() : 0)
                .max()
                .orElse(0);
    }

    private String extractCompilationOutput(List<TestcaseJudgeResult> results) {
        return results.stream()
                .filter(r -> r.verdict() == Verdict.COMPILATION_ERROR)
                .map(TestcaseJudgeResult::compileOutput)
                .findFirst()
                .orElse(null);
    }

    private void persistResults(JudgeResult judgeResult, UUID submissionId, Verdict verdict,
                                int passedCount, List<TestcaseJudgeResult> results) {
        submissionTestcaseService.saveAll(judgeResult,
                submissionService.getOrThrowSubmission(submissionId));

        submissionService.updateWithJudgeResult(
                submissionId,
                verdict,
                passedCount,
                maxCpuTime(results) > 0 ? maxCpuTime(results) : null,
                maxMemory(results) > 0 ? maxMemory(results) : null,
                extractCompilationOutput(results)
        );
    }
}

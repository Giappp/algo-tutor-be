package org.rap.algotutorbe.judge;

import lombok.extern.slf4j.Slf4j;
import org.rap.algotutorbe.common.errors.ErrorCode;
import org.rap.algotutorbe.common.exception.AppException;
import org.rap.algotutorbe.common.services.BaseService;
import org.rap.algotutorbe.iam.domain.model.User;
import org.rap.algotutorbe.iam.domain.repositories.UserRepository;
import org.rap.algotutorbe.judge.dto.JudgeRequest;
import org.rap.algotutorbe.judge.dto.JudgeResponse;
import org.rap.algotutorbe.judge.dto.PistonResponse;
import org.rap.algotutorbe.judge.dto.PistonStage;
import org.rap.algotutorbe.learning.enums.ProgrammingLanguage;
import org.rap.algotutorbe.learning.models.CodingLesson;
import org.rap.algotutorbe.learning.models.Lesson;
import org.rap.algotutorbe.learning.models.Testcase;
import org.rap.algotutorbe.learning.repositories.CodingLessonRepository;
import org.rap.algotutorbe.learning.repositories.LessonRepository;
import org.rap.algotutorbe.submission.entities.Submission;
import org.rap.algotutorbe.submission.entities.SubmissionTestcase;
import org.rap.algotutorbe.submission.entities.Verdict;
import org.rap.algotutorbe.submission.repositories.SubmissionRepository;
import org.rap.algotutorbe.submission.repositories.SubmissionTestcaseRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Core judge service: executes user code against testcases via Piston,
 * evaluates results, and persists submissions.
 * Testcases are executed in parallel for performance.
 */
@Slf4j
@Service
public class JudgeService extends BaseService {

    private static final int COMPILE_TIMEOUT_MS = 10_000;

    private final LessonRepository lessonRepository;
    private final CodingLessonRepository codingLessonRepository;
    private final PistonClient pistonClient;
    private final UserRepository userRepository;
    private final LessonProgressUpdater lessonProgressUpdater;
    private final SubmissionRepository submissionRepository;
    private final SubmissionTestcaseRepository submissionTestcaseRepository;
    private final Executor judgeExecutor;

    public JudgeService(
            LessonRepository lessonRepository,
            CodingLessonRepository codingLessonRepository,
            PistonClient pistonClient,
            UserRepository userRepository,
            LessonProgressUpdater lessonProgressUpdater,
            SubmissionRepository submissionRepository,
            SubmissionTestcaseRepository submissionTestcaseRepository,
            @Qualifier("judgeExecutor") Executor judgeExecutor
    ) {
        this.lessonRepository = lessonRepository;
        this.codingLessonRepository = codingLessonRepository;
        this.pistonClient = pistonClient;
        this.userRepository = userRepository;
        this.lessonProgressUpdater = lessonProgressUpdater;
        this.submissionRepository = submissionRepository;
        this.submissionTestcaseRepository = submissionTestcaseRepository;
        this.judgeExecutor = judgeExecutor;
    }

    // ─── Public API ──────────────────────────────────────────────────────────────

    /**
     * Run code against visible testcases only. Does NOT persist.
     */
    @Transactional(readOnly = true)
    public JudgeResponse run(JudgeRequest request) {
        ProgrammingLanguage language = parseLanguage(request.language());
        CodingLesson lesson = findCodingLesson(request.lessonSlug());

        List<Testcase> visible = lesson.getTestCases().stream()
                .filter(tc -> !Boolean.TRUE.equals(tc.getIsHidden()))
                .toList();

        if (visible.isEmpty()) {
            return emptyResponse();
        }

        return judge(language, request.code(), lesson, visible);
    }

    /**
     * Submit code against ALL testcases. Persists submission + updates progress.
     */
    @Transactional
    public JudgeResponse submit(JudgeRequest request) {
        User user = userRepository.findById(getCurrentUserIdOrThrow())
                .orElseThrow(() -> new AppException(ErrorCode.ACCESS_DENIED));

        ProgrammingLanguage language = parseLanguage(request.language());
        CodingLesson lesson = findCodingLesson(request.lessonSlug());

        List<Testcase> allTestCases = lesson.getTestCases();
        if (allTestCases == null || allTestCases.isEmpty()) {
            throw new AppException(ErrorCode.PROBLEM_NOT_FOUND);
        }

        JudgeResponse result = judge(language, request.code(), lesson, allTestCases);

        Verdict verdict = Verdict.valueOf(result.verdict());
        Submission submission = saveSubmission(user, lesson, request.code(), language, result, verdict);
        saveTestcaseResults(submission, result.testCases());

        boolean progressUpdated = verdict == Verdict.ACCEPTED
                && lessonProgressUpdater.markLessonCompleted(user, lesson);

        return new JudgeResponse(
                submission.getId().toString(),
                result.verdict(),
                result.summary(),
                result.performance(),
                result.testCases(),
                result.compilationError(),
                progressUpdated
        );
    }

    // ─── Core judge logic ────────────────────────────────────────────────────────

    private JudgeResponse judge(
            ProgrammingLanguage language, String code,
            CodingLesson lesson, List<Testcase> testcases
    ) {
        int runTimeout = language.calculatePistonRunTimeout(lesson.getBaseTimeLimitMs());
        int memoryLimit = language.calculateMemoryLimit(lesson.getBaseMemoryLimitMb());

        List<TestCaseExecution> executions = executeAllParallel(
                language, code, testcases, runTimeout, memoryLimit
        );

        return aggregate(executions);
    }

    private List<TestCaseExecution> executeAllParallel(
            ProgrammingLanguage language, String code,
            List<Testcase> testcases, int runTimeout, int memoryLimit
    ) {
        List<CompletableFuture<TestCaseExecution>> futures = new ArrayList<>(testcases.size());

        for (int i = 0; i < testcases.size(); i++) {
            Testcase tc = testcases.get(i);
            int index = tc.getOrderIndex() != null && tc.getOrderIndex() >= 0 ? tc.getOrderIndex() : i;
            boolean hidden = Boolean.TRUE.equals(tc.getIsHidden());

            futures.add(CompletableFuture.supplyAsync(
                    () -> executeOne(language, code, tc, index, hidden, runTimeout, memoryLimit),
                    judgeExecutor
            ));
        }

        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();

        return futures.stream().map(CompletableFuture::join).toList();
    }

    private TestCaseExecution executeOne(
            ProgrammingLanguage language, String code, Testcase tc,
            int index, boolean hidden, int runTimeout, int memoryLimit
    ) {
        PistonResponse response = pistonClient.executeRaw(
                language, code, tc.getStdin(), runTimeout, COMPILE_TIMEOUT_MS, memoryLimit
        );

        if (response == null) {
            return error(index, tc, hidden, Verdict.SYSTEM_ERROR, "No response from execution engine");
        }

        if (hasCompileError(response)) {
            return compilationError(response.compile().stderr());
        }

        PistonStage run = response.run();
        if (run == null) {
            return error(index, tc, hidden, Verdict.SYSTEM_ERROR, "No run output from execution engine");
        }

        Integer displayTime = run.cpuTime() != null ? language.adjustExecutionTime(run.cpuTime()) : null;
        Integer memory = run.memory();

        if (run.code() != 0) {
            Verdict verdict = Verdict.fromPistonSignal(run.signal());
            String msg = run.stderr() != null && !run.stderr().isBlank() ? run.stderr() : run.signal();
            return result(index, tc, hidden, run.stdout(), verdict, displayTime, memory, msg);
        }

        String actual = run.stdout();
        boolean passed = normalize(actual).equals(normalize(tc.getExpectedStdout()));
        Verdict verdict = passed ? Verdict.ACCEPTED : Verdict.WRONG_ANSWER;

        return result(index, tc, hidden, actual, verdict, displayTime, memory, null);
    }

    // ─── Aggregation ─────────────────────────────────────────────────────────────

    private JudgeResponse aggregate(List<TestCaseExecution> executions) {
        List<JudgeResponse.TestCaseResult> results = new ArrayList<>();
        int totalTime = 0;
        int maxMemory = 0;
        String compileError = null;
        Verdict finalVerdict = Verdict.ACCEPTED;
        int passed = 0;

        for (TestCaseExecution exec : executions) {
            if (exec.verdict() == Verdict.COMPILATION_ERROR) {
                compileError = exec.compilationError();
                finalVerdict = Verdict.COMPILATION_ERROR;
                break;
            }

            if (exec.result() != null) results.add(exec.result());
            if (exec.timeMs() != null) totalTime += exec.timeMs();
            if (exec.memoryKb() != null && exec.memoryKb() > maxMemory) maxMemory = exec.memoryKb();

            if (exec.verdict() == Verdict.ACCEPTED) {
                passed++;
            } else if (finalVerdict == Verdict.ACCEPTED) {
                finalVerdict = exec.verdict();
            }
        }

        int total = executions.size();
        if (finalVerdict == Verdict.COMPILATION_ERROR) {
            passed = 0;
        }

        return new JudgeResponse(
                null,
                finalVerdict.name(),
                new JudgeResponse.Summary(passed, total - passed, total),
                new JudgeResponse.Performance(
                        totalTime > 0 ? totalTime : null,
                        maxMemory > 0 ? maxMemory : null
                ),
                results,
                compileError,
                null
        );
    }

    // ─── TestCaseExecution builders ──────────────────────────────────────────────

    private TestCaseExecution result(
            int index, Testcase tc, boolean hidden,
            String actual, Verdict verdict, Integer timeMs, Integer memoryKb, String errorMsg
    ) {
        var tcResult = new JudgeResponse.TestCaseResult(
                index, verdict.name(),
                hidden ? null : tc.getStdin(),
                hidden ? null : tc.getExpectedStdout(),
                hidden ? null : actual,
                timeMs, memoryKb, hidden, errorMsg
        );
        return new TestCaseExecution(tcResult, verdict, null, timeMs, memoryKb);
    }

    private TestCaseExecution error(int index, Testcase tc, boolean hidden, Verdict verdict, String msg) {
        return result(index, tc, hidden, null, verdict, null, null, msg);
    }

    private TestCaseExecution compilationError(String stderr) {
        return new TestCaseExecution(null, Verdict.COMPILATION_ERROR, stderr, null, null);
    }

    // ─── Persistence ─────────────────────────────────────────────────────────────

    private Submission saveSubmission(
            User user, CodingLesson lesson, String code,
            ProgrammingLanguage language, JudgeResponse response, Verdict verdict
    ) {
        Submission submission = new Submission();
        submission.setUser(user);
        submission.setCodingLesson(lesson);
        submission.setSourceCode(code);
        submission.setLanguage(language);
        submission.setVerdict(verdict);
        submission.setPassedTestcases(response.summary().passed());
        submission.setTotalTestcases(response.summary().total());
        submission.setExecutionTime(response.performance().totalTimeMs());
        submission.setMemoryUsed(response.performance().maxMemoryKb());
        submission.setCompileOutput(response.compilationError());
        return submissionRepository.save(submission);
    }

    private void saveTestcaseResults(Submission submission, List<JudgeResponse.TestCaseResult> results) {
        List<SubmissionTestcase> entities = results.stream().map(r -> {
            SubmissionTestcase entity = new SubmissionTestcase();
            entity.setSubmission(submission);
            entity.setTestcaseIndex(r.index());
            entity.setVerdict(Verdict.valueOf(r.status()));
            entity.setTime(r.timeMs() != null ? r.timeMs().doubleValue() : null);
            entity.setMemory(r.memoryKb());
            entity.setStdout(r.actualOutput());
            return entity;
        }).toList();

        submissionTestcaseRepository.saveAll(entities);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────────

    private boolean hasCompileError(PistonResponse response) {
        return response.compile() != null && response.compile().code() != 0;
    }

    private CodingLesson findCodingLesson(String slug) {
        Lesson lesson = lessonRepository.findBySlug(slug)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));

        if (!(lesson instanceof CodingLesson)) {
            throw new AppException(ErrorCode.INVALID_LESSON_TYPE);
        }

        return codingLessonRepository.findByIdWithTestCases(lesson.getId())
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));
    }

    private ProgrammingLanguage parseLanguage(String lang) {
        try {
            return ProgrammingLanguage.fromApiValue(lang);
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.UNSUPPORTED_PROGRAMMING_LANGUAGE);
        }
    }

    private String normalize(String s) {
        return s == null ? "" : s.replace("\r\n", "\n").trim();
    }

    private JudgeResponse emptyResponse() {
        return new JudgeResponse(null, Verdict.ACCEPTED.name(),
                new JudgeResponse.Summary(0, 0, 0),
                new JudgeResponse.Performance(null, null),
                List.of(), null, null);
    }

    // ─── Internal ────────────────────────────────────────────────────────────────

    private record TestCaseExecution(
            JudgeResponse.TestCaseResult result,
            Verdict verdict,
            String compilationError,
            Integer timeMs,
            Integer memoryKb
    ) {
    }
}

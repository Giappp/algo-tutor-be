package org.rap.algotutorbe.judge;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rap.algotutorbe.common.errors.ErrorCode;
import org.rap.algotutorbe.common.exception.AppException;
import org.rap.algotutorbe.common.services.BaseService;
import org.rap.algotutorbe.iam.domain.model.User;
import org.rap.algotutorbe.iam.domain.repositories.UserRepository;
import org.rap.algotutorbe.judge.dto.JudgeRunRequest;
import org.rap.algotutorbe.judge.dto.JudgeRunResponse;
import org.rap.algotutorbe.judge.dto.JudgeSubmitResponse;
import org.rap.algotutorbe.judge.dto.PistonResponse;
import org.rap.algotutorbe.learning.enums.ProgrammingLanguage;
import org.rap.algotutorbe.learning.models.CodingLesson;
import org.rap.algotutorbe.learning.models.Lesson;
import org.rap.algotutorbe.learning.models.Testcase;
import org.rap.algotutorbe.learning.repositories.CodingLessonRepository;
import org.rap.algotutorbe.learning.repositories.LessonRepository;
import org.rap.algotutorbe.submission.entities.Submission;
import org.rap.algotutorbe.submission.entities.Verdict;
import org.rap.algotutorbe.submission.repositories.SubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Synchronous judge service for the frontend learning flow.
 * Handles POST /judge/run (visible test cases) and POST /judge/submit (all test cases).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JudgeRunService extends BaseService {

    private static final int DEFAULT_COMPILE_TIMEOUT_MS = 10_000;

    private final LessonRepository lessonRepository;
    private final CodingLessonRepository codingLessonRepository;
    private final PistonClient pistonClient;
    private final UserRepository userRepository;
    private final LessonProgressUpdater lessonProgressUpdater;
    private final SubmissionRepository submissionRepository;

    /**
     * Run code against visible test cases only (POST /judge/run).
     */
    @Transactional(readOnly = true)
    public JudgeRunResponse run(JudgeRunRequest request) {
        ProgrammingLanguage language = parseLanguage(request.language());
        CodingLesson codingLesson = findCodingLesson(request.lessonSlug());

        List<Testcase> visibleTestCases = codingLesson.getTestCases().stream()
                .filter(tc -> !Boolean.TRUE.equals(tc.getIsHidden()))
                .toList();

        if (visibleTestCases.isEmpty()) {
            return new JudgeRunResponse(List.of(), 0, null);
        }

        int baseTimeLimit = codingLesson.getBaseTimeLimitMs();
        int baseMemoryLimit = codingLesson.getBaseMemoryLimitMb();
        int runTimeout = language.calculatePistonRunTimeout(baseTimeLimit);
        int memoryLimit = language.calculateMemoryLimit(baseMemoryLimit);

        List<JudgeRunResponse.TestCaseRunResult> results = new ArrayList<>();
        int totalTime = 0;
        String compilationError = null;

        for (Testcase tc : visibleTestCases) {
            PistonResponse response = pistonClient.executeRaw(
                    language, request.code(), tc.getStdin(),
                    runTimeout, DEFAULT_COMPILE_TIMEOUT_MS, memoryLimit
            );

            if (response == null) {
                results.add(new JudgeRunResponse.TestCaseRunResult(
                        tc.getStdin(), tc.getExpectedStdout(), null, false, false, null));
                continue;
            }

            // Compilation error — stop early
            if (response.compile() != null && response.compile().code() != 0) {
                compilationError = response.compile().stderr();
                break;
            }

            var run = response.run();
            if (run == null) {
                results.add(new JudgeRunResponse.TestCaseRunResult(
                        tc.getStdin(), tc.getExpectedStdout(), null, false, false, null));
                continue;
            }

            String actual = run.stdout();
            boolean passed = normalize(actual).equals(normalize(tc.getExpectedStdout()));
            Integer execTime = run.cpuTime();
            if (execTime != null) totalTime += execTime;

            results.add(new JudgeRunResponse.TestCaseRunResult(
                    tc.getStdin(), tc.getExpectedStdout(), actual, passed, false, execTime));
        }

        return new JudgeRunResponse(results, totalTime, compilationError);
    }

    /**
     * Submit code against ALL test cases (POST /judge/submit).
     * Auto-updates lesson progress if ACCEPTED.
     */
    @Transactional
    public JudgeSubmitResponse submit(JudgeRunRequest request) {
        UUID userId = getCurrentUserIdOrThrow();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.ACCESS_DENIED));

        ProgrammingLanguage language = parseLanguage(request.language());
        CodingLesson codingLesson = findCodingLesson(request.lessonSlug());

        List<Testcase> allTestCases = codingLesson.getTestCases();
        if (allTestCases == null || allTestCases.isEmpty()) {
            throw new AppException(ErrorCode.PROBLEM_NOT_FOUND);
        }

        int baseTimeLimit = codingLesson.getBaseTimeLimitMs();
        int baseMemoryLimit = codingLesson.getBaseMemoryLimitMb();
        int runTimeout = language.calculatePistonRunTimeout(baseTimeLimit);
        int memoryLimit = language.calculateMemoryLimit(baseMemoryLimit);

        List<JudgeSubmitResponse.TestCaseSubmitResult> results = new ArrayList<>();
        int totalTime = 0;
        int maxMemory = 0;
        String compilationError = null;
        Verdict finalVerdict = Verdict.ACCEPTED;
        int passedCount = 0;

        for (Testcase tc : allTestCases) {
            PistonResponse response = pistonClient.executeRaw(
                    language, request.code(), tc.getStdin(),
                    runTimeout, DEFAULT_COMPILE_TIMEOUT_MS, memoryLimit
            );

            if (response == null) {
                finalVerdict = Verdict.SYSTEM_ERROR;
                results.add(buildSubmitResult(tc, null, false, null));
                continue;
            }

            // Compilation error
            if (response.compile() != null && response.compile().code() != 0) {
                compilationError = response.compile().stderr();
                finalVerdict = Verdict.COMPILATION_ERROR;
                break;
            }

            var run = response.run();
            if (run == null) {
                finalVerdict = Verdict.SYSTEM_ERROR;
                results.add(buildSubmitResult(tc, null, false, null));
                continue;
            }

            Integer execTime = run.cpuTime();
            Integer memory = run.memory();
            if (execTime != null) totalTime += execTime;
            if (memory != null && memory > maxMemory) maxMemory = memory;

            // Runtime error
            if (run.code() != 0) {
                Verdict tcVerdict = Verdict.fromPistonSignal(run.signal());
                if (finalVerdict == Verdict.ACCEPTED) finalVerdict = tcVerdict;
                results.add(buildSubmitResult(tc, run.stdout(), false, execTime));
                continue;
            }

            String actual = run.stdout();
            boolean passed = normalize(actual).equals(normalize(tc.getExpectedStdout()));

            if (passed) {
                passedCount++;
            } else if (finalVerdict == Verdict.ACCEPTED) {
                finalVerdict = Verdict.WRONG_ANSWER;
            }

            results.add(buildSubmitResult(tc, actual, passed, execTime));
        }

        // Save submission record
        Submission submission = new Submission();
        submission.setUser(user);
        submission.setCodingLesson(codingLesson);
        submission.setSourceCode(request.code());
        submission.setLanguage(language);
        submission.setVerdict(finalVerdict);
        submission.setPassedTestcases(passedCount);
        submission.setTotalTestcases(allTestCases.size());
        submission.setExecutionTime(totalTime > 0 ? totalTime : null);
        submission.setMemoryUsed(maxMemory > 0 ? maxMemory : null);
        submission.setCompileOutput(compilationError);
        Submission saved = submissionRepository.save(submission);

        // Auto-update progress if ACCEPTED
        boolean progressUpdated = false;
        if (finalVerdict == Verdict.ACCEPTED) {
            progressUpdated = lessonProgressUpdater.markLessonCompleted(user, codingLesson);
        }

        return new JudgeSubmitResponse(
                saved.getId().toString(),
                finalVerdict.name(),
                results,
                totalTime,
                maxMemory > 0 ? maxMemory : null,
                compilationError,
                progressUpdated
        );
    }

    // ── Private helpers ──────────────────────────────────────────

    private JudgeSubmitResponse.TestCaseSubmitResult buildSubmitResult(
            Testcase tc, String actual, boolean passed, Integer execTime) {
        boolean hidden = Boolean.TRUE.equals(tc.getIsHidden());
        return new JudgeSubmitResponse.TestCaseSubmitResult(
                hidden ? null : tc.getStdin(),
                hidden ? null : tc.getExpectedStdout(),
                hidden ? null : actual,
                passed,
                hidden,
                execTime
        );
    }

    private CodingLesson findCodingLesson(String slug) {
        Lesson lesson = lessonRepository.findBySlug(slug)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));

        if (!(lesson instanceof CodingLesson)) {
            throw new AppException(ErrorCode.INVALID_LESSON_TYPE);
        }

        // Re-fetch with test cases eagerly loaded
        return codingLessonRepository.findByIdWithTestCases(lesson.getId())
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));
    }

    private ProgrammingLanguage parseLanguage(String language) {
        try {
            return ProgrammingLanguage.fromApiValue(language);
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.UNSUPPORTED_PROGRAMMING_LANGUAGE);
        }
    }

    private String normalize(String s) {
        if (s == null) return "";
        return s.replace("\r\n", "\n").trim();
    }
}

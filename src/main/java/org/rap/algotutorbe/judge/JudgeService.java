package org.rap.algotutorbe.judge;

import lombok.extern.slf4j.Slf4j;
import org.rap.algotutorbe.common.errors.ErrorCode;
import org.rap.algotutorbe.common.exception.AppException;
import org.rap.algotutorbe.common.services.BaseService;
import org.rap.algotutorbe.common.services.S3Service;
import org.rap.algotutorbe.iam.domain.model.User;
import org.rap.algotutorbe.iam.domain.repositories.UserRepository;
import org.rap.algotutorbe.judge.dto.JudgeRequest;
import org.rap.algotutorbe.judge.dto.JudgeResponse;
import org.rap.algotutorbe.judge.dto.PistonResponse;
import org.rap.algotutorbe.learning.dto.testcase.TestCaseResultWebSocketResponse;
import org.rap.algotutorbe.learning.enums.ProgrammingLanguage;
import org.rap.algotutorbe.learning.models.CodingLesson;
import org.rap.algotutorbe.learning.models.Testcase;
import org.rap.algotutorbe.learning.repositories.CodingLessonRepository;
import org.rap.algotutorbe.learning.services.LessonProgressUpdater;
import org.rap.algotutorbe.submission.entities.Submission;
import org.rap.algotutorbe.submission.entities.SubmissionDetail;
import org.rap.algotutorbe.submission.entities.Verdict;
import org.rap.algotutorbe.submission.repositories.SubmissionDetailRepository;
import org.rap.algotutorbe.submission.repositories.SubmissionRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Slf4j
@Service
public class JudgeService extends BaseService {

    private static final int COMPILE_TIMEOUT_MS = 10_000;
    private static final int DEFAULT_TIME_LIMIT_MS = 2_000;
    private static final int DEFAULT_MEMORY_LIMIT_MB = 256;

    private final CodingLessonRepository codingLessonRepository;
    private final PistonClient pistonClient;
    private final UserRepository userRepository;
    private final LessonProgressUpdater lessonProgressUpdater;
    private final SubmissionRepository submissionRepository;
    private final SubmissionDetailRepository submissionDetailRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final S3Service s3Service;
    private final Executor judgeExecutor;

    public JudgeService(
            CodingLessonRepository codingLessonRepository,
            PistonClient pistonClient,
            UserRepository userRepository,
            LessonProgressUpdater lessonProgressUpdater,
            SubmissionRepository submissionRepository,
            SubmissionDetailRepository submissionDetailRepository,
            SimpMessagingTemplate messagingTemplate,
            S3Service s3Service,
            @Qualifier("judgeExecutor") Executor judgeExecutor
    ) {
        this.codingLessonRepository = codingLessonRepository;
        this.pistonClient = pistonClient;
        this.userRepository = userRepository;
        this.lessonProgressUpdater = lessonProgressUpdater;
        this.submissionRepository = submissionRepository;
        this.submissionDetailRepository = submissionDetailRepository;
        this.messagingTemplate = messagingTemplate;
        this.s3Service = s3Service;
        this.judgeExecutor = judgeExecutor;
    }

    @Transactional(readOnly = true)
    public JudgeResponse run(JudgeRequest request) {
        ProgrammingLanguage language = parseLanguage(request.language());
        CodingLesson lesson = findCodingLesson(request.lessonSlug());

        List<Testcase> sampleTestcases = lesson.getTestCases() != null
                ? lesson.getTestCases().stream()
                .filter(testcase -> Boolean.TRUE.equals(testcase.getIsSample()))
                .toList()
                : List.of();

        if (sampleTestcases.isEmpty()) {
            return emptyResponse();
        }

        JudgingResult result = judgeTestCases(language, request.code(), lesson, sampleTestcases, false, null);

        return toRunResponse(result);
    }

    public JudgeResponse submit(JudgeRequest request) {
        User user = findCurrentUser();
        ProgrammingLanguage language = parseLanguage(request.language());
        CodingLesson lesson = findCodingLesson(request.lessonSlug());

        List<Testcase> testcases = lesson.getTestCases();
        if (testcases == null || testcases.isEmpty()) {
            throw new AppException(ErrorCode.PROBLEM_NOT_FOUND);
        }

        Submission submission = createPendingSubmission(user, lesson, request.code(), language);
        UUID submissionId = submission.getId();

        CompletableFuture.runAsync(
                () -> executeSubmitJudging(submissionId, user, language, request.code(), lesson, testcases),
                judgeExecutor
        );

        return new JudgeResponse(
                submissionId.toString(),
                Verdict.PENDING.name(),
                null,
                null,
                null,
                null,
                false
        );
    }

    private void executeSubmitJudging(
            UUID submissionId,
            User user,
            ProgrammingLanguage language,
            String code,
            CodingLesson lesson,
            List<Testcase> testcases
    ) {
        JudgingResult result = judgeTestCases(
                language,
                code,
                lesson,
                testcases,
                true,
                (testcase, testCaseResult, completed) ->
                        saveDetailAndNotify(submissionId, testcase, testCaseResult, completed)
        );

        finalizeSubmission(submissionId, user, lesson, result);
    }

    private JudgingResult judgeTestCases(
            ProgrammingLanguage language,
            String code,
            CodingLesson lesson,
            List<Testcase> testcases,
            boolean stopOnFailure,
            TestCaseResultListener listener
    ) {
        int maxTimeMs = 0;
        int maxMemoryKb = 0;
        int passedCount = 0;
        Verdict finalVerdict = Verdict.ACCEPTED;
        String compilationError = null;

        List<JudgeResponse.TestCaseResult> responseResults = new ArrayList<>();

        for (int index = 0; index < testcases.size(); index++) {
            Testcase testcase = testcases.get(index);
            boolean isLastTestcase = index == testcases.size() - 1;

            SingleTestCaseResult result = executeTestCase(language, code, lesson, testcase);

            maxTimeMs = Math.max(maxTimeMs, result.timeMs());
            maxMemoryKb = Math.max(maxMemoryKb, result.memoryKb());

            if (result.verdict() == Verdict.ACCEPTED) {
                passedCount++;
            } else if (finalVerdict == Verdict.ACCEPTED) {
                finalVerdict = result.verdict();
            }

            if (result.verdict() == Verdict.COMPILATION_ERROR) {
                compilationError = result.stderr();
            }

            responseResults.add(toHttpTestCaseResult(index, result));

            boolean mustStop = result.verdict() == Verdict.COMPILATION_ERROR
                    || result.verdict() == Verdict.SYSTEM_ERROR
                    || (stopOnFailure && result.verdict() != Verdict.ACCEPTED);
            boolean completed = isLastTestcase || mustStop;
            notifyListener(listener, testcase, result, completed);

            if (mustStop) {
                break;
            }
        }

        return new JudgingResult(
                finalVerdict,
                passedCount,
                testcases.size(),
                maxTimeMs,
                maxMemoryKb,
                compilationError,
                responseResults
        );
    }

    private SingleTestCaseResult executeTestCase(
            ProgrammingLanguage language,
            String code,
            CodingLesson lesson,
            Testcase testcase
    ) {
        try {
            String stdin = readTestcaseFile(lesson, testcase.getInputFileUrl());
            String expectedOutput = readTestcaseFile(lesson, testcase.getOutputFileUrl());

            JudgeLimits limits = resolveLimits(language, lesson);

            PistonResponse response = pistonClient.executeRaw(
                    language,
                    code,
                    stdin,
                    limits.timeLimitMs(),
                    COMPILE_TIMEOUT_MS,
                    limits.memoryLimitMb()
            );

            if (response == null) {
                return new SingleTestCaseResult(
                        Verdict.SYSTEM_ERROR,
                        0,
                        0,
                        "",
                        "No response from judging engine"
                );
            }

            if (hasCompileError(response)) {
                return new SingleTestCaseResult(
                        Verdict.COMPILATION_ERROR,
                        0,
                        0,
                        "",
                        stageError(response.compile(), "Compilation error")
                );
            }

            if (response.run() == null) {
                return new SingleTestCaseResult(
                        Verdict.SYSTEM_ERROR,
                        0,
                        0,
                        "",
                        "No execution result from judging engine"
                );
            }

            int timeMs = cleanTimeMs(language, response.run().cpuTime(), response.run().wallTime());
            int memoryKb = bytesToKb(response.run().memory());
            String stdout = cleanStream(response.run().stdout());
            String stderr = stageError(response.run(), "");

            if (hasStageFailed(response.run())) {
                return new SingleTestCaseResult(
                        resolveFailureVerdict(response.run(), memoryKb, limits),
                        timeMs,
                        memoryKb,
                        stdout,
                        stderr
                );
            }

            if (timeMs > limits.baseTimeLimitMs()) {
                return new SingleTestCaseResult(
                        Verdict.TIME_LIMIT_EXCEEDED,
                        timeMs,
                        memoryKb,
                        stdout,
                        stderr
                );
            }

            Verdict verdict = normalizeForComparison(stdout).equals(normalizeForComparison(expectedOutput))
                    ? Verdict.ACCEPTED
                    : Verdict.WRONG_ANSWER;

            return new SingleTestCaseResult(
                    verdict,
                    timeMs,
                    memoryKb,
                    stdout,
                    stderr
            );
        } catch (Exception e) {
            log.error("Judge testcase failed: {}", e.getMessage(), e);

            return new SingleTestCaseResult(
                    Verdict.SYSTEM_ERROR,
                    0,
                    0,
                    "",
                    "Unable to execute testcase"
            );
        }
    }

    private JudgeResponse.TestCaseResult toHttpTestCaseResult(int index, SingleTestCaseResult result) {
        return new JudgeResponse.TestCaseResult(
                index,
                result.verdict().name(),
                result.timeMs(),
                result.memoryKb(),
                result.stdout(),
                result.stderr()
        );
    }

    private JudgeResponse toRunResponse(JudgingResult result) {
        return new JudgeResponse(
                null,
                result.finalVerdict().name(),
                new JudgeResponse.Summary(
                        result.passedCount(),
                        result.totalCount(),
                        result.testCaseResults().size() - result.passedCount(),
                        result.testCaseResults().size()
                ),
                new JudgeResponse.Performance(
                        result.maxTimeMs(),
                        result.maxMemoryKb()
                ),
                result.testCaseResults(),
                result.compilationError(),
                false
        );
    }

    private Submission createPendingSubmission(
            User user,
            CodingLesson lesson,
            String code,
            ProgrammingLanguage language
    ) {
        Submission submission = new Submission();
        submission.setUser(user);
        submission.setCodingLesson(lesson);
        submission.setSourceCode(code);
        submission.setLanguage(language);
        submission.setVerdict(Verdict.PENDING);

        return submissionRepository.save(submission);
    }

    private void saveDetailAndNotify(
            UUID submissionId,
            Testcase testcase,
            SingleTestCaseResult result,
            boolean completed
    ) {
        saveSubmissionDetail(submissionId, testcase, result);
        notifyTestCaseResult(submissionId, testcase, result, completed);
    }

    private void saveSubmissionDetail(
            UUID submissionId,
            Testcase testcase,
            SingleTestCaseResult result
    ) {
        try {
            Submission submission = submissionRepository.findById(submissionId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy Submission ID"));

            SubmissionDetail detail = new SubmissionDetail();
            detail.setSubmission(submission);
            detail.setTestcase(testcase);
            detail.setVerdict(result.verdict());
            detail.setTime(result.timeMs());
            detail.setMemory(result.memoryKb());
            detail.setStdout(result.stdout());
            detail.setStderr(result.stderr());

            submissionDetailRepository.save(detail);
        } catch (Exception e) {
            log.error("Save submission detail failed: {}", e.getMessage(), e);
        }
    }

    private void notifyTestCaseResult(
            UUID submissionId,
            Testcase testcase,
            SingleTestCaseResult result,
            boolean completed
    ) {
        TestCaseResultWebSocketResponse response = TestCaseResultWebSocketResponse.builder()
                .type("TEST_CASE")
                .submissionId(submissionId)
                .testCaseId(testcase.getId())
                .status(result.verdict().name())
                .timeMs(result.timeMs())
                .memoryKb(result.memoryKb())
                .stdout(result.stdout())
                .stderr(result.stderr())
                .sortOrder(testcase.getSortOrder() != null ? testcase.getSortOrder() : 0)
                .isCompleted(completed)
                .build();

        messagingTemplate.convertAndSend(submissionTopic(submissionId), response);
    }

    private void finalizeSubmission(
            UUID submissionId,
            User user,
            CodingLesson lesson,
            JudgingResult result
    ) {
        try {
            Submission submission = submissionRepository.findById(submissionId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy Submission ID"));

            submission.setVerdict(result.finalVerdict());
            submission.setPassedTestcases(result.passedCount());
            submission.setTotalTestcases(result.totalCount());
            submission.setExecutionTime(result.maxTimeMs());
            submission.setMemoryUsed(result.maxMemoryKb());
            submission.setCompileOutput(result.compilationError());

            submissionRepository.save(submission);

            if (result.finalVerdict() == Verdict.ACCEPTED) {
                lessonProgressUpdater.markLessonCompleted(user, lesson);
            }

            notifyFinalResult(submissionId, result);
        } catch (Exception e) {
            log.error("Finalize submission failed: {}", e.getMessage(), e);
        }
    }

    private void notifyFinalResult(UUID submissionId, JudgingResult result) {
        TestCaseResultWebSocketResponse response = TestCaseResultWebSocketResponse.builder()
                .type("FINAL_RESULT")
                .submissionId(submissionId)
                .status(result.finalVerdict().name())
                .passed(result.passedCount())
                .total(result.totalCount())
                .maxTimeMs(result.maxTimeMs())
                .maxMemoryKb(result.maxMemoryKb())
                .compilationError(result.compilationError())
                .isCompleted(true)
                .build();

        messagingTemplate.convertAndSend(submissionTopic(submissionId), response);
    }

    private String readTestcaseFile(CodingLesson lesson, String fileUrl) throws IOException {
        Path path = s3Service.getOrCreateLocalTestCaseFile(lesson.getId(), fileUrl);
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private JudgeLimits resolveLimits(ProgrammingLanguage language, CodingLesson lesson) {
        int baseTimeLimitMs = lesson.getBaseTimeLimitMs() != null
                ? lesson.getBaseTimeLimitMs()
                : DEFAULT_TIME_LIMIT_MS;

        int baseMemoryLimitMb = lesson.getBaseMemoryLimitMb() != null
                ? lesson.getBaseMemoryLimitMb()
                : DEFAULT_MEMORY_LIMIT_MB;

        return new JudgeLimits(
                baseTimeLimitMs,
                language.calculatePistonRunTimeout(baseTimeLimitMs),
                language.calculateMemoryLimit(baseMemoryLimitMb)
        );
    }

    private User findCurrentUser() {
        return userRepository.findById(getCurrentUserIdOrThrow())
                .orElseThrow(() -> new AppException(ErrorCode.ACCESS_DENIED));
    }

    private CodingLesson findCodingLesson(String slug) {
        return codingLessonRepository.findBySlugWithTestCases(slug)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));
    }

    private ProgrammingLanguage parseLanguage(String language) {
        try {
            return ProgrammingLanguage.fromApiValue(language);
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.UNSUPPORTED_PROGRAMMING_LANGUAGE);
        }
    }

    private boolean hasCompileError(PistonResponse response) {
        return hasStageFailed(response.compile());
    }

    private boolean hasStageFailed(org.rap.algotutorbe.judge.dto.PistonStage stage) {
        return stage != null
                && ((stage.code() != null && stage.code() != 0)
                || (stage.signal() != null && !stage.signal().isBlank()));
    }

    private String cleanStream(String value) {
        return value == null ? "" : value.replace("\r\n", "\n");
    }

    private String normalizeForComparison(String value) {
        return cleanStream(value).trim();
    }

    private String stageError(org.rap.algotutorbe.judge.dto.PistonStage stage, String fallback) {
        if (stage == null) {
            return fallback;
        }
        String stderr = cleanStream(stage.stderr());
        if (!stderr.isBlank()) {
            return stderr;
        }
        String output = cleanStream(stage.output());
        return !output.isBlank() ? output : fallback;
    }

    private int cleanTimeMs(ProgrammingLanguage language, Integer cpuTime, Integer wallTime) {
        int rawTimeMs = cpuTime != null ? cpuTime : wallTime != null ? wallTime : 0;
        return rawTimeMs > 0 ? language.adjustExecutionTime(rawTimeMs) : 0;
    }

    private int bytesToKb(Integer bytes) {
        return bytes == null || bytes <= 0 ? 0 : (int) Math.ceil(bytes / 1024.0);
    }

    private Verdict resolveFailureVerdict(
            org.rap.algotutorbe.judge.dto.PistonStage run,
            int memoryKb,
            JudgeLimits limits
    ) {
        if (memoryKb >= limits.memoryLimitMb() * 1024) {
            return Verdict.MEMORY_LIMIT_EXCEEDED;
        }
        if (run.signal() != null && !run.signal().isBlank()) {
            return Verdict.fromPistonSignal(run.signal());
        }
        return Verdict.RUNTIME_ERROR;
    }

    private String submissionTopic(UUID submissionId) {
        return "/topic/submissions/" + submissionId;
    }

    private void notifyListener(
            TestCaseResultListener listener,
            Testcase testcase,
            SingleTestCaseResult result,
            boolean completed
    ) {
        if (listener != null) {
            listener.onResult(testcase, result, completed);
        }
    }

    private JudgeResponse emptyResponse() {
        return new JudgeResponse(
                null,
                Verdict.ACCEPTED.name(),
                new JudgeResponse.Summary(0, 0, 0, 0),
                new JudgeResponse.Performance(0, 0),
                List.of(),
                null,
                false
        );
    }

    @FunctionalInterface
    private interface TestCaseResultListener {
        void onResult(Testcase testcase, SingleTestCaseResult result, boolean completed);
    }

    private record JudgeLimits(
            int baseTimeLimitMs,
            int timeLimitMs,
            int memoryLimitMb
    ) {
    }

    private record SingleTestCaseResult(
            Verdict verdict,
            int timeMs,
            int memoryKb,
            String stdout,
            String stderr
    ) {
    }

    private record JudgingResult(
            Verdict finalVerdict,
            int passedCount,
            int totalCount,
            int maxTimeMs,
            int maxMemoryKb,
            String compilationError,
            List<JudgeResponse.TestCaseResult> testCaseResults
    ) {
    }
}

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

        List<Testcase> sampleTestcases = lesson.getTestCases().stream()
                .filter(testcase -> Boolean.TRUE.equals(testcase.getIsSample()))
                .toList();

        if (sampleTestcases.isEmpty()) {
            return emptyResponse();
        }

        JudgingResult result = judgeTestCases(language, request.code(), lesson, sampleTestcases, null);

        return toRunResponse(result);
    }

    @Transactional
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
            TestCaseResultListener listener
    ) {
        int maxTimeMs = 0;
        int maxMemoryKb = 0;
        int passedCount = 0;
        Verdict finalVerdict = Verdict.ACCEPTED;
        String compileError = null;

        List<JudgeResponse.TestCaseResult> responseResults = new ArrayList<>();

        for (int index = 0; index < testcases.size(); index++) {
            Testcase testcase = testcases.get(index);
            boolean isLastTestcase = index == testcases.size() - 1;

            SingleTestCaseResult result = executeTestCase(language, code, lesson, testcase);

            maxTimeMs = Math.max(maxTimeMs, result.timeMs());
            maxMemoryKb = Math.max(maxMemoryKb, result.memoryKb());

            if (result.verdict() == Verdict.ACCEPTED) {
                passedCount++;
            } else {
                finalVerdict = result.verdict();
            }

            if (result.verdict() == Verdict.COMPILATION_ERROR) {
                compileError = result.output();
            }

            responseResults.add(toHttpTestCaseResult(index, result));

            boolean completed = isLastTestcase || result.verdict() != Verdict.ACCEPTED;
            notifyListener(listener, testcase, result, completed);

            if (result.verdict() != Verdict.ACCEPTED) {
                break;
            }
        }

        return new JudgingResult(
                finalVerdict,
                passedCount,
                testcases.size(),
                maxTimeMs,
                maxMemoryKb,
                compileError,
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

            JudgeLimits limits = resolveLimits(lesson);

            PistonResponse response = pistonClient.executeRaw(
                    language,
                    code,
                    stdin,
                    limits.timeLimitMs(),
                    COMPILE_TIMEOUT_MS,
                    limits.memoryLimitMb()
            );

            if (hasCompileError(response)) {
                return new SingleTestCaseResult(
                        Verdict.COMPILATION_ERROR,
                        0,
                        0,
                        response.compile().stderr()
                );
            }

            int timeMs = valueOrZero(response.run().cpuTime());
            int memoryKb = valueOrZero(response.run().memory()) / 1024;

            if (response.run().code() != 0) {
                return new SingleTestCaseResult(
                        Verdict.RUNTIME_ERROR,
                        timeMs,
                        memoryKb,
                        response.run().stderr()
                );
            }

            String actualOutput = normalize(response.run().stdout());
            Verdict verdict = actualOutput.equals(normalize(expectedOutput))
                    ? Verdict.ACCEPTED
                    : Verdict.WRONG_ANSWER;

            return new SingleTestCaseResult(
                    verdict,
                    timeMs,
                    memoryKb,
                    actualOutput
            );
        } catch (Exception e) {
            log.error("Judge testcase failed: {}", e.getMessage(), e);

            return new SingleTestCaseResult(
                    Verdict.SYSTEM_ERROR,
                    0,
                    0,
                    ""
            );
        }
    }

    private JudgeResponse.TestCaseResult toHttpTestCaseResult(int index, SingleTestCaseResult result) {
        return new JudgeResponse.TestCaseResult(
                index,
                result.verdict().name(),
                result.timeMs(),
                result.memoryKb(),
                result.output()
        );
    }

    private JudgeResponse toRunResponse(JudgingResult result) {
        return new JudgeResponse(
                null,
                result.finalVerdict().name(),
                new JudgeResponse.Summary(
                        result.passedCount(),
                        result.totalCount(),
                        result.totalCount() - result.passedCount()
                ),
                new JudgeResponse.Performance(
                        result.maxTimeMs(),
                        result.maxMemoryKb()
                ),
                result.testCaseResults(),
                result.compileError(),
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
                .runTimeMs(result.timeMs())
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
            submission.setCompileOutput(result.compileError());

            submissionRepository.save(submission);

            if (result.finalVerdict() == Verdict.ACCEPTED) {
                lessonProgressUpdater.markLessonCompleted(user, lesson);
            }

            notifyFinalResult(submissionId, result.finalVerdict());
        } catch (Exception e) {
            log.error("Finalize submission failed: {}", e.getMessage(), e);
        }
    }

    private void notifyFinalResult(UUID submissionId, Verdict verdict) {
        TestCaseResultWebSocketResponse response = TestCaseResultWebSocketResponse.builder()
                .type("FINAL_RESULT")
                .submissionId(submissionId)
                .status(verdict.name())
                .isCompleted(true)
                .build();

        messagingTemplate.convertAndSend(submissionTopic(submissionId), response);
    }

    private String readTestcaseFile(CodingLesson lesson, String fileUrl) throws IOException {
        Path path = s3Service.getOrCreateLocalTestCaseFile(lesson.getId(), fileUrl);
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private JudgeLimits resolveLimits(CodingLesson lesson) {
        int timeLimitMs = lesson.getBaseTimeLimitMs() != null
                ? lesson.getBaseTimeLimitMs()
                : DEFAULT_TIME_LIMIT_MS;

        int memoryLimitMb = lesson.getBaseMemoryLimitMb() != null
                ? lesson.getBaseMemoryLimitMb()
                : DEFAULT_MEMORY_LIMIT_MB;

        return new JudgeLimits(timeLimitMs, memoryLimitMb);
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
        return response.compile() != null && response.compile().code() != 0;
    }

    private String normalize(String value) {
        return value == null ? "" : value.replace("\r\n", "\n").trim();
    }

    private int valueOrZero(Integer value) {
        return value != null ? value : 0;
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
                new JudgeResponse.Summary(0, 0, 0),
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
            int timeLimitMs,
            int memoryLimitMb
    ) {
    }

    private record SingleTestCaseResult(
            Verdict verdict,
            int timeMs,
            int memoryKb,
            String output
    ) {
    }

    private record JudgingResult(
            Verdict finalVerdict,
            int passedCount,
            int totalCount,
            int maxTimeMs,
            int maxMemoryKb,
            String compileError,
            List<JudgeResponse.TestCaseResult> testCaseResults
    ) {
    }
}
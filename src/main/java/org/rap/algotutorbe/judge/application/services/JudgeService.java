package org.rap.algotutorbe.judge.application.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rap.algotutorbe.judge.application.dto.JudgeResult;
import org.rap.algotutorbe.judge.application.dto.PistonExecutionResponse;
import org.rap.algotutorbe.judge.application.dto.TestcaseResultDto;
import org.rap.algotutorbe.problem.application.dto.TestcaseDto;
import org.rap.algotutorbe.problem.application.services.ProblemService;
import org.rap.algotutorbe.problem.domain.models.Problem;
import org.rap.algotutorbe.submission.SubmissionCreatedMessage;
import org.rap.algotutorbe.submission.application.service.SubmissionService;
import org.rap.algotutorbe.submission.application.service.SubmissionTestcaseService;
import org.rap.algotutorbe.submission.domain.model.Submission;
import org.rap.algotutorbe.submission.domain.model.SubmissionStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class JudgeService {

    private static final Set<String> KILL_SIGNALS = Set.of("SIGKILL", "SIGTERM", "SIGXCPU");
    private final PistonClient pistonClient;
    private final ProblemService problemService;
    private final SubmissionService submissionService;             // <-- thêm
    private final SubmissionTestcaseService submissionTestcaseService;

    public JudgeResult processSubmission(SubmissionCreatedMessage message) {
        log.info("Bắt đầu chấm bài: submissionId={}", message.submissionId());

        JudgeContext ctx = fetchContext(message);
        JudgeResult result = runTestcases(message, ctx);

        persistTestcaseResults(message.submissionId(), result); // <-- thêm

        return result;
    }

    private void persistTestcaseResults(Long submissionId, JudgeResult result) {
        Submission submission = submissionService.getOrThrowSubmission(submissionId);
        submissionTestcaseService.saveAll(result, submission);
    }

    // ─── Orchestration ────────────────────────────────────────────────────────

    private JudgeContext fetchContext(SubmissionCreatedMessage message) {
        Problem problem = problemService.findById(message.problemId());
        List<TestcaseDto> testcases = problemService.getProblemTestcase(message.problemId());
        int runTimeoutMs = message.programmingLanguage().calculatePistonRunTimeout(problem.getBaseTimeLimitMs());
        int memoryLimitMb = message.programmingLanguage().calculateMemoryLimit(problem.getBaseMemoryLimitMb());
        return new JudgeContext(problem, testcases, runTimeoutMs, memoryLimitMb);
    }

    private JudgeResult runTestcases(SubmissionCreatedMessage message, JudgeContext ctx) {
        SubmissionStatus finalStatus = SubmissionStatus.ACCEPTED;
        int passedCount = 0;
        long maxCpuTime = 0;
        long maxMemory = 0;
        List<TestcaseResultDto> details = new ArrayList<>();

        for (int i = 0; i < ctx.testcases().size(); i++) {
            TestcaseResultDto result = evaluateSingleTestcase(
                    message, ctx, ctx.testcases().get(i), i + 1
            );

            maxCpuTime = Math.max(maxCpuTime, result.cpuTime());
            maxMemory = Math.max(maxMemory, result.memory());
            details.add(result);

            SubmissionStatus status = SubmissionStatus.valueOf(result.status());
            if (status != SubmissionStatus.ACCEPTED) {
                finalStatus = status;
                break; // fail-fast
            }
            passedCount++;
        }

        return new JudgeResult(finalStatus, passedCount, ctx.testcases().size(),
                maxCpuTime, maxMemory, details);
    }

    // ─── Per-testcase evaluation ──────────────────────────────────────────────

    private TestcaseResultDto evaluateSingleTestcase(
            SubmissionCreatedMessage message,
            JudgeContext ctx,
            TestcaseDto testcase,
            int index
    ) {
        PistonExecutionResponse response = pistonClient.executeCode(
                message.programmingLanguage(),
                message.sourceCode(),
                testcase.input(),
                ctx.runTimeoutMs(),
                ctx.memoryLimitMb()
        );

        ResourceMetrics metrics = extractMetrics(response);
        SubmissionStatus status = resolveStatus(response, metrics, ctx, testcase);

        return new TestcaseResultDto(index, status.toString(), metrics.cpuTime(), metrics.memory());
    }

    private SubmissionStatus resolveStatus(
            PistonExecutionResponse response,
            ResourceMetrics metrics,
            JudgeContext ctx,
            TestcaseDto testcase
    ) {
        SubmissionStatus status = determinePistonStatus(response);
        if (status != SubmissionStatus.ACCEPTED) return status;

        status = checkResourceLimits(metrics, ctx);
        if (status != SubmissionStatus.ACCEPTED) return status;

        String stdout = response.getRun().getStdout();
        return compareOutput(stdout, testcase.expectedOutput())
                ? SubmissionStatus.ACCEPTED
                : SubmissionStatus.WRONG_ANSWER;
    }

    // ─── Pure helpers ─────────────────────────────────────────────────────────

    private SubmissionStatus determinePistonStatus(PistonExecutionResponse response) {
        if (hasCompileError(response)) return SubmissionStatus.COMPILE_ERROR;
        if (isKilledByTimeout(response)) return SubmissionStatus.TIME_LIMIT_EXCEEDED;
        if (response.getRun().getCode() != 0) return SubmissionStatus.RUNTIME_ERROR;
        return SubmissionStatus.ACCEPTED;
    }

    private SubmissionStatus checkResourceLimits(ResourceMetrics metrics, JudgeContext ctx) {
        if (metrics.cpuTime() > ctx.runTimeoutMs()) return SubmissionStatus.TIME_LIMIT_EXCEEDED;
        if (metrics.memory() > ctx.memoryLimitMb()) return SubmissionStatus.MEMORY_LIMIT_EXCEEDED;
        return SubmissionStatus.ACCEPTED;
    }

    private boolean hasCompileError(PistonExecutionResponse response) {
        return response.getCompile() != null && response.getCompile().getCode() != 0;
    }

    private boolean isKilledByTimeout(PistonExecutionResponse response) {
        return KILL_SIGNALS.contains(response.getRun().getSignal());
    }

    private boolean compareOutput(String actual, String expected) {
        if (actual == null || expected == null) return false;
        return normalize(actual).equals(normalize(expected));
    }

    private String normalize(String s) {
        return s.trim().replace("\r\n", "\n");
    }

    private ResourceMetrics extractMetrics(PistonExecutionResponse response) {
        long cpuTime = Optional.ofNullable(response.getRun().getCpuTime()).orElse(0L);
        long memory = Optional.ofNullable(response.getRun().getMemory()).orElse(0L);
        return new ResourceMetrics(cpuTime, memory);
    }

    // ─── Inner records ────────────────────────────────────────────────────────

    private record JudgeContext(
            Problem problem,
            List<TestcaseDto> testcases,
            int runTimeoutMs,
            int memoryLimitMb
    ) {
    }

    private record ResourceMetrics(long cpuTime, long memory) {
    }
}

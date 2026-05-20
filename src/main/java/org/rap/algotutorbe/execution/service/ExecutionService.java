package org.rap.algotutorbe.execution.service;

import lombok.RequiredArgsConstructor;
import org.rap.algotutorbe.common.errors.ErrorCode;
import org.rap.algotutorbe.common.exception.AppException;
import org.rap.algotutorbe.execution.dto.*;
import org.rap.algotutorbe.judge.PistonClient;
import org.rap.algotutorbe.judge.dto.PistonResponse;
import org.rap.algotutorbe.learning.enums.ProgrammingLanguage;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for free-form code execution (sandbox/playground).
 * Unlike JudgeService, this does not validate against lesson testcases.
 */
@Service
@RequiredArgsConstructor
public class ExecutionService {
    private static final int DEFAULT_RUN_TIMEOUT_MS = 3_000;
    private static final int DEFAULT_COMPILE_TIMEOUT_MS = 5_000;
    private static final int DEFAULT_MEMORY_LIMIT_MB = 256;

    private final PistonClient pistonClient;

    /**
     * Execute code with optional stdin (playground mode).
     */
    public ExecuteResponse execute(ExecuteRequest request) {
        ProgrammingLanguage language = parseLanguage(request.language());
        int timeout = request.timeout() != null ? request.timeout() : DEFAULT_RUN_TIMEOUT_MS;

        PistonResponse response = pistonClient.executeRaw(
                language, request.code(), request.stdin(),
                timeout, DEFAULT_COMPILE_TIMEOUT_MS, DEFAULT_MEMORY_LIMIT_MB
        );

        if (response == null) {
            return new ExecuteResponse("", "No response from execution engine", 1, null, null);
        }

        if (response.compile() != null && response.compile().code() != 0) {
            return new ExecuteResponse(
                    "",
                    response.compile().stderr(),
                    response.compile().code(),
                    response.compile().cpuTime(),
                    response.compile().memory()
            );
        }

        var run = response.run();
        if (run == null) {
            return new ExecuteResponse("", "No run output from execution engine", 1, null, null);
        }

        return new ExecuteResponse(
                run.stdout(),
                run.stderr(),
                run.code(),
                run.cpuTime(),
                run.memory()
        );
    }

    /**
     * Execute code against user-provided test cases (custom test mode).
     */
    public ExecuteTestResponse executeWithTestcases(ExecuteTestRequest request) {
        ProgrammingLanguage language = parseLanguage(request.language());
        List<ExecuteTestcase> testcases = request.testCases() != null ? request.testCases() : List.of();

        if (testcases.isEmpty()) {
            return new ExecuteTestResponse(List.of(), new ExecuteTestSummary(0, 0, 0));
        }

        List<ExecuteTestcaseResult> results = new ArrayList<>();
        int passed = 0;

        for (ExecuteTestcase tc : testcases) {
            PistonResponse resp = pistonClient.executeRaw(
                    language, request.code(), tc.input(),
                    DEFAULT_RUN_TIMEOUT_MS, DEFAULT_COMPILE_TIMEOUT_MS, DEFAULT_MEMORY_LIMIT_MB
            );

            String actual = resp != null && resp.run() != null ? resp.run().stdout() : null;
            Integer execTime = resp != null && resp.run() != null ? resp.run().cpuTime() : null;

            boolean ok = normalize(actual).equals(normalize(tc.expected()));
            if (ok) passed++;

            results.add(new ExecuteTestcaseResult(
                    tc.id(),
                    ok ? "PASSED" : "FAILED",
                    tc.input(),
                    tc.expected(),
                    actual,
                    execTime
            ));
        }

        int total = testcases.size();
        int failed = total - passed;

        return new ExecuteTestResponse(results, new ExecuteTestSummary(passed, failed, total));
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

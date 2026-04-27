package org.rap.algotutorbe.execution.service;

import lombok.RequiredArgsConstructor;
import org.rap.algotutorbe.execution.dto.*;
import org.rap.algotutorbe.judge.PistonClient;
import org.rap.algotutorbe.judge.dto.PistonResponse;
import org.rap.algotutorbe.problem.domain.enums.ProgrammingLanguage;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExecutionService {
    private static final int DEFAULT_COMPILE_TIMEOUT_MS = 10_000;
    private static final int DEFAULT_MEMORY_LIMIT_MB = 256;

    private final PistonClient pistonClient;

    public ExecuteResponse execute(ExecuteRequest request) {
        ProgrammingLanguage language;
        try {
            language = ProgrammingLanguage.fromApiValue(request.language());
        } catch (IllegalArgumentException e) {
            return new ExecuteResponse("", "Unsupported programming language", 1, null, null);
        }
        int timeout = request.timeout() == null ? 3000 : request.timeout();

        PistonResponse response = pistonClient.executeRaw(
                language,
                request.code(),
                request.stdin(),
                timeout,
                DEFAULT_COMPILE_TIMEOUT_MS,
                DEFAULT_MEMORY_LIMIT_MB
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
        return new ExecuteResponse(
                run.stdout(),
                run.stderr(),
                run.code(),
                run.cpuTime(),
                run.memory()
        );
    }

    public ExecuteTestResponse executeWithTestcases(ExecuteTestRequest request) {
        ProgrammingLanguage language;
        try {
            language = ProgrammingLanguage.fromApiValue(request.language());
        } catch (IllegalArgumentException e) {
            return new ExecuteTestResponse(List.of(), new ExecuteTestSummary(0, 0, 0));
        }
        List<ExecuteTestcase> tcs = request.testCases() == null ? List.of() : request.testCases();

        List<ExecuteTestcaseResult> results = new ArrayList<>();
        int passed = 0;

        for (ExecuteTestcase tc : tcs) {
            PistonResponse resp = pistonClient.executeRaw(
                    language,
                    request.code(),
                    tc.input(),
                    5000,
                    DEFAULT_COMPILE_TIMEOUT_MS,
                    DEFAULT_MEMORY_LIMIT_MB
            );

            String actual = resp != null && resp.run() != null ? resp.run().stdout() : null;
            Integer execTime = resp != null && resp.run() != null ? resp.run().cpuTime() : null;

            boolean ok = normalize(actual).equals(normalize(tc.expected()));
            if (ok) passed++;

            results.add(new ExecuteTestcaseResult(
                    tc.id(),
                    ok ? "passed" : "failed",
                    tc.input(),
                    tc.expected(),
                    actual,
                    execTime
            ));
        }

        int total = tcs.size();
        int failed = total - passed;

        return new ExecuteTestResponse(results, new ExecuteTestSummary(passed, failed, total));
    }

    private String normalize(String s) {
        if (s == null) return "";
        return s.replace("\r\n", "\n").trim();
    }
}


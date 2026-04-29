package org.rap.algotutorbe.judge;

import lombok.extern.slf4j.Slf4j;
import org.rap.algotutorbe.judge.dto.*;
import org.rap.algotutorbe.judge.exception.JudgeConnectionException;
import org.rap.algotutorbe.judge.exception.JudgeException;
import org.rap.algotutorbe.judge.exception.PistonApiException;
import org.rap.algotutorbe.learning.enums.ProgrammingLanguage;
import org.rap.algotutorbe.learning.models.Testcase;
import org.rap.algotutorbe.submission.entities.Verdict;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;

import static org.rap.algotutorbe.submission.entities.Verdict.*;

@Slf4j
@Component
public class PistonClient {

    private static final String EXECUTE_ENDPOINT = "/api/v2/execute";
    private static final String DEFAULT_VERSION = "*";

    private final RestClient restClient;

    public PistonClient(RestClient.Builder builder,
                        @Value("${piston.api.url}") String pistonApiUrl) {
        HttpClient jdkHttpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(jdkHttpClient);
        factory.setReadTimeout(Duration.ofSeconds(60));

        this.restClient = builder
                .baseUrl(pistonApiUrl)
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .requestFactory(factory)
                .build();
    }

    // ── Admin validation (existing) ──────────────────────

    public PistonResponse executeRaw(
            ProgrammingLanguage language,
            String code,
            String stdin,
            int runTimeoutMs,
            int compileTimeoutMs,
            int memoryLimitMb
    ) {
        PistonRequest request = buildRequest(language, code, stdin, runTimeoutMs, compileTimeoutMs, memoryLimitMb);
        return sendRequest(request);
    }

    public ValidationDetail executeCode(
            int index,
            ProgrammingLanguage language,
            String code,
            Testcase testCase,
            int runTimeoutMs,
            int compileTimeoutMs,
            int memoryLimitMb
    ) {
        PistonRequest request = buildRequest(language, code, testCase.getStdin(), runTimeoutMs, compileTimeoutMs, memoryLimitMb);
        PistonResponse response = sendRequest(request);

        if (response == null) {
            return createErrorDetail(index, testCase, SYSTEM_ERROR, "Khong nhan duoc phan hoi tu Piston");
        }
        if (response.compile() != null && response.compile().code() != 0) {
            return createErrorDetail(index, testCase, COMPILATION_ERROR, response.compile().stderr());
        }
        PistonStage run = response.run();
        if (run == null) {
            return createErrorDetail(index, testCase, SYSTEM_ERROR, "Piston tra ve phan hoi khong hop le (khong co run output)");
        }
        if (run.code() != 0) {
            String errorMsg = run.stderr() != null ? run.stderr() : run.signal();
            return createErrorDetail(index, testCase, RUNTIME_ERROR, errorMsg);
        }

        String actualOutput = run.stdout();
        boolean isMatch = normalizeOutput(actualOutput).equals(normalizeOutput(testCase.getExpectedStdout()));

        return isMatch
                ? new ValidationDetail(index, ACCEPTED, testCase.getStdin(), testCase.getExpectedStdout(), actualOutput, null)
                : new ValidationDetail(index, WRONG_ANSWER, testCase.getStdin(), testCase.getExpectedStdout(), actualOutput, "Output khong khop");
    }


    public TestcaseJudgeResult executeForJudging(
            int index,
            ProgrammingLanguage language,
            String code,
            Testcase testcase,
            int runTimeoutMs,
            int compileTimeoutMs,
            int memoryLimitMb
    ) {
        PistonRequest request = buildRequest(language, code, testcase.getStdin(), runTimeoutMs, compileTimeoutMs, memoryLimitMb);
        PistonResponse response = sendRequest(request);

        if (response == null) {
            return new TestcaseJudgeResult(index, SYSTEM_ERROR, 0, 0, null, null);
        }
        if (response.compile() != null && response.compile().code() != 0) {
            return new TestcaseJudgeResult(index, COMPILATION_ERROR, 0, 0, null, response.compile().stderr());
        }

        PistonStage run = response.run();
        if (run == null) {
            return new TestcaseJudgeResult(index, SYSTEM_ERROR, 0, 0, null, "No run output from Piston");
        }
        Integer cpuTime = run.cpuTime() != null ? run.cpuTime() : 0;
        Integer memory = run.memory() != null ? run.memory() : 0;

        if (run.code() != 0) {
            Verdict verdict = fromPistonSignal(run.signal());
            return new TestcaseJudgeResult(index, verdict, cpuTime, memory, run.stderr(), null);
        }

        boolean isMatch = normalizeOutput(run.stdout()).equals(normalizeOutput(testcase.getExpectedStdout()));
        Verdict verdict = isMatch ? ACCEPTED : WRONG_ANSWER;
        return new TestcaseJudgeResult(index, verdict, cpuTime, memory, run.stdout(), null);
    }

    // ── Private helpers ──────────────────────────────────

    private ValidationDetail createErrorDetail(int index, Testcase testCase, Verdict verdict, String errorMsg) {
        return new ValidationDetail(index, verdict, testCase.getStdin(), testCase.getExpectedStdout(), null, errorMsg);
    }

    private String normalizeOutput(String output) {
        if (output == null) return "";
        return output.replace("\\r\\n", "\n").trim();
    }

    private PistonRequest buildRequest(
            ProgrammingLanguage language,
            String sourceCode,
            String stdin,
            int runTimeoutMs,
            int compileTimeoutMs,
            int memoryLimitMb
    ) {
        PistonFile file = new PistonFile(language.getFileName(), sourceCode);
        return new PistonRequest(
                language.getPistonAlias(),
                DEFAULT_VERSION,
                List.of(file),
                stdin != null ? stdin : "",
                runTimeoutMs,
                compileTimeoutMs,
                memoryLimitMb
        );
    }

    private PistonResponse sendRequest(PistonRequest request) {
        try {
            return restClient.post()
                    .uri(EXECUTE_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(PistonResponse.class);

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("Piston API error — status: {}, body: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new PistonApiException(
                    "Lỗi từ hệ thống chấm bài",
                    e.getStatusCode().value(),
                    e.getResponseBodyAsString()
            );
        } catch (ResourceAccessException e) {
            log.error("Network error while connecting to Piston: {}", e.getMessage());
            throw new JudgeConnectionException(
                    "Không thể kết nối đến máy chủ chấm bài, vui lòng thử lại sau.", e
            );
        } catch (IllegalArgumentException e) {
            log.error("Invalid piston request configuration: {}", e.getMessage());
            throw new JudgeException("Cấu hình yêu cầu không hợp lệ: " + e.getMessage(), e);
        }
    }
}
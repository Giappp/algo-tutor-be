package org.rap.algotutorbe.judge;

import lombok.extern.slf4j.Slf4j;
import org.rap.algotutorbe.judge.dto.PistonFile;
import org.rap.algotutorbe.judge.dto.PistonRequest;
import org.rap.algotutorbe.judge.dto.PistonResponse;
import org.rap.algotutorbe.judge.exception.JudgeConnectionException;
import org.rap.algotutorbe.judge.exception.JudgeException;
import org.rap.algotutorbe.judge.exception.PistonApiException;
import org.rap.algotutorbe.learning.enums.ProgrammingLanguage;
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

/**
 * HTTP client for the Piston code execution API.
 * Single responsibility: send code to Piston and return raw response.
 */
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

    /**
     * Execute code via Piston API and return the raw response.
     *
     * @return PistonResponse or null if an unexpected error occurs
     */
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

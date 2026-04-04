package org.rap.algotutorbe.judge.application.services;

import lombok.extern.slf4j.Slf4j;
import org.rap.algotutorbe.judge.application.dto.PistonExecutionResponse;
import org.rap.algotutorbe.judge.application.dto.PistonFile;
import org.rap.algotutorbe.judge.application.dto.PistonRequest;
import org.rap.algotutorbe.judge.domain.exception.JudgeConnectionException;
import org.rap.algotutorbe.judge.domain.exception.JudgeException;
import org.rap.algotutorbe.judge.domain.exception.PistonApiException;
import org.rap.algotutorbe.problem.domain.enums.ProgrammingLanguage;
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
        factory.setReadTimeout(Duration.ofSeconds(15));

        this.restClient = builder
                .baseUrl(pistonApiUrl)
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .requestFactory(factory)
                .build();
    }

    public PistonExecutionResponse executeCode(
            ProgrammingLanguage language,
            String sourceCode,
            String stdin,
            int runTimeoutMs,
            int memoryLimitMb
    ) {
        PistonRequest request = buildRequest(language, sourceCode, stdin, runTimeoutMs, memoryLimitMb);
        log.info("Sending execution request to Piston. Language: {}", language);
        return sendRequest(request);
    }

    private PistonRequest buildRequest(
            ProgrammingLanguage language,
            String sourceCode,
            String stdin,
            int runTimeoutMs,
            int memoryLimitMb
    ) {
        PistonFile file = new PistonFile(language.getFileName(), sourceCode);
        return new PistonRequest(
                language.getPistonAlias(),
                DEFAULT_VERSION,
                List.of(file),
                stdin != null ? stdin : "",
                runTimeoutMs,
                memoryLimitMb
        );
    }

    private PistonExecutionResponse sendRequest(PistonRequest request) {
        try {
            return restClient.post()
                    .uri(EXECUTE_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(PistonExecutionResponse.class);

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
        } catch (Exception e) {
            log.error("Unexpected error in PistonClient: {}", e.getMessage(), e);
            throw new JudgeException("Lỗi hệ thống bất ngờ trong quá trình chấm bài", e);
        }
    }
}
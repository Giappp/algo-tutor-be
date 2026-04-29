package org.rap.algotutorbe.judge;

import lombok.extern.slf4j.Slf4j;
import org.rap.algotutorbe.judge.dto.ValidationDetail;
import org.rap.algotutorbe.judge.dto.ValidationResult;
import org.rap.algotutorbe.learning.enums.ProgrammingLanguage;
import org.rap.algotutorbe.learning.models.Testcase;
import org.rap.algotutorbe.submission.entities.Verdict;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.IntStream;

@Slf4j
@Service
@EnableAsync
@Configuration
public class PistonValidationService {
    private static final int DEFAULT_TIMEOUT_MS = 3000;
    private static final int COMPILE_TIMEOUT_MS = 5000;
    private static final int MEMORY_LIMIT_MB = 256;

    private final PistonClient pistonClient;

    public PistonValidationService(PistonClient pistonClient) {
        this.pistonClient = pistonClient;
    }

    @Bean(name = "pistonValidationExecutor")
    public Executor pistonValidationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("piston-validate-");
        executor.initialize();
        return executor;
    }

    public ValidationResult validateSolution(
            ProgrammingLanguage language,
            String solutionCode,
            List<Testcase> testCases
    ) {
        List<CompletableFuture<ValidationDetail>> futures = IntStream.range(0, testCases.size())
                .mapToObj(index -> CompletableFuture.supplyAsync(
                        () -> pistonClient.executeCode(
                                index, language, solutionCode,
                                testCases.get(index),
                                DEFAULT_TIMEOUT_MS, COMPILE_TIMEOUT_MS, MEMORY_LIMIT_MB
                        )
                ))
                .toList();

        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();

        List<ValidationDetail> details = futures.stream()
                .map(CompletableFuture::join)
                .toList();

        boolean allPassed = details.stream()
                .allMatch(d -> d.verdict() == Verdict.ACCEPTED);

        return new ValidationResult(allPassed, details);
    }
}

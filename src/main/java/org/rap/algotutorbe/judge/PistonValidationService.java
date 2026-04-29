package org.rap.algotutorbe.judge;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.rap.algotutorbe.judge.dto.ValidationDetail;
import org.rap.algotutorbe.judge.dto.ValidationResult;
import org.rap.algotutorbe.learning.enums.ProgrammingLanguage;
import org.rap.algotutorbe.learning.models.Testcase;
import org.rap.algotutorbe.submission.entities.Verdict;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class PistonValidationService {
    private static final int DEFAULT_TIMEOUT_MS = 3000;
    private static final int COMPILE_TIMEOUT_MS = 5000;
    private static final int MEMORY_LIMIT_MB = 256;

    private final ExecutorService executor = Executors.newFixedThreadPool(10);
    private final PistonClient pistonClient;

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
                        ),
                        executor
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

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
    }
}

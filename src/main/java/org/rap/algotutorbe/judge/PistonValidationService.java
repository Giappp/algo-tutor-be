package org.rap.algotutorbe.judge;

import lombok.RequiredArgsConstructor;
import org.rap.algotutorbe.judge.dto.ValidationDetail;
import org.rap.algotutorbe.judge.dto.ValidationResult;
import org.rap.algotutorbe.problem.application.dto.request.TestcaseRequest;
import org.rap.algotutorbe.problem.domain.enums.ProgrammingLanguage;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class PistonValidationService {
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);
    private final PistonClient pistonClient;

    public ValidationResult validateSolution(ProgrammingLanguage language, String solutionCode, List<TestcaseRequest> testCases) {
        List<CompletableFuture<ValidationDetail>> futures = IntStream.range(0, testCases.size())
                .mapToObj(index -> CompletableFuture.supplyAsync(
                        () -> pistonClient.executeCode(index, language, solutionCode, testCases.get(index), 3000, 5000, 256),
                        executorService
                ))
                .toList();
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        List<ValidationDetail> details = futures.stream()
                .map(CompletableFuture::join)
                .toList();

        boolean allPassed = details.stream()
                .allMatch(d -> d.status().equals("ACCEPTED"));

        return new ValidationResult(allPassed, details);
    }
}

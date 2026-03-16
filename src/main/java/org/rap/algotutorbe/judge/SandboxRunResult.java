package org.rap.algotutorbe.judge;

import java.util.List;

public record SandboxRunResult(
        boolean allPassed,
        long maxObservedTimeMs,
        long maxObservedMemoryMb,
        List<SandboxTestResult> testResults
) {
}

package org.rap.algotutorbe.judge;

public record SandboxTestResult(
        int index,
        boolean passed,
        long timeMs,
        long memoryMb,
        String stdout,
        String stderr
) {
}

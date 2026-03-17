package org.rap.algotutorbe.judge;

import java.util.List;

public record SandboxRunRequest(
        String sourceCode,
        String language,
        List<SandboxTestInput> testInputs,
        long timeLimitMs,
        long memoryLimitMb
) {
}
package org.rap.algotutorbe.learning.enums;

import lombok.Getter;

@Getter
public enum ProgrammingLanguage {
    JAVA("java", "Main.java", 2.0, 256, 800),
    PYTHON("python", "main.py", 3.0, 64, 0);

    private final String pistonAlias;
    private final String fileName;
    private final double timeMultiplier;
    private final int extraMemoryMb;
    /**
     * Estimated runtime overhead (ms) for language startup (e.g., JVM cold start).
     * Subtracted from reported execution time for fairer FE display.
     */
    private final int runtimeOverheadMs;

    ProgrammingLanguage(String pistonAlias, String fileName,
                        double timeMultiplier, int extraMemoryMb, int runtimeOverheadMs) {
        this.pistonAlias = pistonAlias;
        this.fileName = fileName;
        this.timeMultiplier = timeMultiplier;
        this.extraMemoryMb = extraMemoryMb;
        this.runtimeOverheadMs = runtimeOverheadMs;
    }

    public static ProgrammingLanguage fromApiValue(String value) {
        if (value == null) return null;
        String normalized = value.trim().toLowerCase();
        for (ProgrammingLanguage lang : values()) {
            if (lang.pistonAlias.equals(normalized)) return lang;
        }
        throw new IllegalArgumentException("Unsupported programming language: " + value);
    }

    public int calculatePistonRunTimeout(int baseTimeLimitMs) {
        int buffer = this == JAVA ? JAVA.runtimeOverheadMs : 0;
        return (int) (baseTimeLimitMs * timeMultiplier) + buffer;
    }

    public int calculateMemoryLimit(int baseMemoryLimitMb) {
        return baseMemoryLimitMb + extraMemoryMb;
    }

    /**
     * Adjusts raw execution time by subtracting estimated runtime overhead.
     * Returns at least 1ms to avoid showing 0 for very fast executions.
     */
    public int adjustExecutionTime(int rawTimeMs) {
        int adjusted = rawTimeMs - runtimeOverheadMs;
        return Math.max(adjusted, 1);
    }

    public String toApiValue() {
        return pistonAlias;
    }
}

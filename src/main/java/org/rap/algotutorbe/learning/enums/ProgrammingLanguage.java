package org.rap.algotutorbe.learning.enums;

import lombok.Getter;

@Getter
public enum ProgrammingLanguage {
    JAVA("java", "Main.java", 2.0, 256),
    PYTHON("python", "main.py", 3.0, 64);

    private final String pistonAlias;
    private final String fileName;
    private final double timeMultiplier;
    private final int extraMemoryMb;

    ProgrammingLanguage(String pistonAlias, String fileName,
                        double timeMultiplier, int extraMemoryMb) {
        this.pistonAlias = pistonAlias;
        this.fileName = fileName;
        this.timeMultiplier = timeMultiplier;
        this.extraMemoryMb = extraMemoryMb;
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
        int buffer = this == JAVA ? 500 : 0;
        return (int) (baseTimeLimitMs * timeMultiplier) + buffer;
    }

    public int calculateMemoryLimit(int baseMemoryLimitMb) {
        return baseMemoryLimitMb + extraMemoryMb;
    }

    public String toApiValue() {
        return pistonAlias;
    }
}

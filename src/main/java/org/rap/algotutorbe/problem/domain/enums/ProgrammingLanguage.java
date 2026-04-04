package org.rap.algotutorbe.problem.domain.enums;

import lombok.Getter;

@Getter
public enum ProgrammingLanguage {
    CPP("c++", "main.cpp", 1.0, 0),
    JAVA("java", "Main.java", 2.0, 256),
    PYTHON("python", "main.py", 3.0, 64);

    private final String pistonAlias;
    private final String fileName;      // <-- thêm field
    private final double timeMultiplier;
    private final int extraMemoryMb;

    ProgrammingLanguage(String pistonAlias, String fileName,
                        double timeMultiplier, int extraMemoryMb) {
        this.pistonAlias = pistonAlias;
        this.fileName = fileName;
        this.timeMultiplier = timeMultiplier;
        this.extraMemoryMb = extraMemoryMb;
    }

    public int calculatePistonRunTimeout(int baseTimeLimitMs) {
        int buffer = this == JAVA ? 500 : 0;
        return (int) (baseTimeLimitMs * timeMultiplier) + buffer;
    }

    public int calculateMemoryLimit(int baseMemoryLimitMb) {
        return baseMemoryLimitMb + extraMemoryMb;
    }
}

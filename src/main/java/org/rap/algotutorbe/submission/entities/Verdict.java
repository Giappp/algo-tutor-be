package org.rap.algotutorbe.submission.entities;

public enum Verdict {
    PENDING,
    PROCESSING,
    ACCEPTED,
    WRONG_ANSWER,
    TIME_LIMIT_EXCEEDED,
    MEMORY_LIMIT_EXCEEDED,
    RUNTIME_ERROR,
    COMPILATION_ERROR,
    SYSTEM_ERROR;

    /**
     * Returns the value stored in the database (lowercase, legacy format).
     */
    public String toDbValue() {
        return switch (this) {
            case PENDING -> "pending";
            case PROCESSING -> "running";
            case ACCEPTED -> "accepted";
            case WRONG_ANSWER -> "wrong_answer";
            case TIME_LIMIT_EXCEEDED -> "time_limit_exceeded";
            case MEMORY_LIMIT_EXCEEDED -> "memory_limit_exceeded";
            case RUNTIME_ERROR -> "runtime_error";
            case COMPILATION_ERROR -> "compile_error";
            case SYSTEM_ERROR -> "system_error";
        };
    }

    /**
     * Returns the API-facing value for frontend consumption (UPPERCASE, matching spec).
     */
    public String toApiValue() {
        return this.name();
    }

    public static Verdict fromApiValue(String value) {
        if (value == null) return null;
        // Support both uppercase (spec), lowercase (legacy DB), and mixed formats
        return switch (value.toLowerCase()) {
            case "pending" -> PENDING;
            case "running", "processing" -> PROCESSING;
            case "accepted" -> ACCEPTED;
            case "wrong_answer" -> WRONG_ANSWER;
            case "time_limit_exceeded" -> TIME_LIMIT_EXCEEDED;
            case "memory_limit_exceeded" -> MEMORY_LIMIT_EXCEEDED;
            case "runtime_error" -> RUNTIME_ERROR;
            case "compile_error", "compilation_error" -> COMPILATION_ERROR;
            case "system_error" -> SYSTEM_ERROR;
            default -> Verdict.valueOf(value.toUpperCase());
        };
    }

    public boolean isTerminal() {
        return this != PENDING && this != PROCESSING;
    }

    public static Verdict fromPistonSignal(String signal) {
        if (signal == null) return RUNTIME_ERROR;
        return switch (signal) {
            case "SIGKILL" -> TIME_LIMIT_EXCEEDED;
            case "SIGXCPU" -> TIME_LIMIT_EXCEEDED;
            case "SIGSEGV" -> RUNTIME_ERROR;
            case "SIGXFSZ" -> MEMORY_LIMIT_EXCEEDED;
            case "SIGFPE" -> RUNTIME_ERROR;
            case "SIGABRT" -> RUNTIME_ERROR;
            default -> RUNTIME_ERROR;
        };
    }
}
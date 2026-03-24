package org.rap.algotutorbe.submission.domain.model;

public enum Verdict {
    PENDING,
    IN_QUEUE,
    PROCESSING,
    ACCEPTED,
    WRONG_ANSWER,
    TIME_LIMIT_EXCEEDED,
    MEMORY_LIMIT_EXCEEDED,
    RUNTIME_ERROR,
    COMPILATION_ERROR,
    SYSTEM_ERROR, SIGSEGV, SIGXFSZ, SIGFPE, SIGABRT, INTERNAL_ERROR;

    public static Verdict fromJudge0StatusId(int statusId) {
        return switch (statusId) {
            case 1 -> IN_QUEUE;
            case 2 -> PROCESSING;
            case 3 -> ACCEPTED;
            case 4 -> WRONG_ANSWER;
            case 5 -> TIME_LIMIT_EXCEEDED;
            case 6 -> COMPILATION_ERROR;
            case 7, 8, 9, 10, 11, 12 -> RUNTIME_ERROR;
            case 13 -> MEMORY_LIMIT_EXCEEDED;
            default -> SYSTEM_ERROR;
        };
    }

    public boolean isTerminal() {
        return this != PENDING && this != IN_QUEUE && this != PROCESSING;
    }
}
package org.rap.algotutorbe.submission.domain.model;

public enum SubmissionStatus {
    PENDING,
    JUDGING,
    ACCEPTED,           // AC
    WRONG_ANSWER,       // WA
    TIME_LIMIT_EXCEEDED,// TLE (Piston trả signal SIGKILL)
    MEMORY_LIMIT_EXCEEDED, // MLE (Piston trả signal SIGSEGV)
    RUNTIME_ERROR,      // RE
    COMPILE_ERROR,      // CE
    SYSTEM_ERROR
}

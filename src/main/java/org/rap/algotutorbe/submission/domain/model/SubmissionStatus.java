package org.rap.algotutorbe.submission.domain.model;

public enum SubmissionStatus {
    PENDING,
    JUDGING,
    ACCEPTED,
    WRONG_ANSWER,
    TIME_LIMIT_EXCEEDED,
    MEMORY_LIMIT_EXCEEDED,
    COMPILE_ERROR,
    RUNTIME_ERROR
}

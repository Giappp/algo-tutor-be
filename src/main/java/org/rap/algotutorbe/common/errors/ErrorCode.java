package org.rap.algotutorbe.common.errors;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    INVALID_PAYLOAD(1, "errors.invalid-payload", HttpStatus.BAD_REQUEST),
    ACCESS_DENIED(2, "errors.access-denied", HttpStatus.UNAUTHORIZED),
    NEED_AUTHENTICATION(3, "errors.need-authentication", HttpStatus.UNAUTHORIZED),
    CONFLICT_RESOURCE(4, "errors.conflict-resource", HttpStatus.CONFLICT),
    NOT_FOUND(5, "errors.not-found", HttpStatus.NOT_FOUND),
    FORBIDDEN(6, "errors.forbidden", HttpStatus.FORBIDDEN),
    INVALID_CREDENTIALS(1000, "error.invalid-credential", HttpStatus.NOT_FOUND),
    EMAIL_ALREADY_INUSE(1001, "errors.email-already-in-use", HttpStatus.BAD_REQUEST),
    USERNAME_TAKEN(1002, "errors.username-taken", HttpStatus.BAD_REQUEST),
    PASSWORD_MISMATCH(1003, "errors.password-mismatch", HttpStatus.NOT_FOUND),
    TOKEN_EXPIRED(1004, "errors.token-expired", HttpStatus.UNAUTHORIZED),
    INVALID_TOKEN(1005, "errors.invalid-token", HttpStatus.UNAUTHORIZED),
    USER_NOT_FOUND(1006, "errors.user-not-found", HttpStatus.NOT_FOUND),

    // Problem Errors
    PROBLEM_NOT_FOUND(2000, "errors.problem-not-found", HttpStatus.NOT_FOUND),
    PROBLEM_ALREADY_EXISTS(2001, "errors.problem-already-exists", HttpStatus.BAD_REQUEST),
    PROBLEM_NOT_PUBLISHED(2002, "errors.problem-not-published", HttpStatus.BAD_REQUEST),
    PROBLEM_TESTCASE_FAILED(2003, "errors.problem-testcase-failed", HttpStatus.BAD_REQUEST),
    EDITORIAL_NOT_FOUND(2004, "errors.editorial-not-found", HttpStatus.NOT_FOUND),
    EDITORIAL_TESTCASE_MISSING(2005, "errors.editorial-testcase-missing", HttpStatus.BAD_REQUEST),

    // Submission Errors
    SUBMISSION_NOT_FOUND(3000, "errors.submission-not-found", HttpStatus.NOT_FOUND),
    SUBMISSION_ACCESS_DENIED(3001, "errors.submission-access-denied", HttpStatus.FORBIDDEN),
    SUBMISSION_COMPILATION_ERROR(3002, "errors.submission-compilation-error", HttpStatus.BAD_REQUEST),
    SUBMISSION_JUDGE_ERROR(3003, "errors.submission-judge-error", HttpStatus.INTERNAL_SERVER_ERROR),
    SUBMISSION_TIMEOUT(3004, "errors.submission-timeout", HttpStatus.REQUEST_TIMEOUT),
    SUBMISSION_TESTCASE_ERROR(3005, "errors.submission-testcase-error", HttpStatus.BAD_REQUEST),
    UNSUPPORTED_PROGRAMMING_LANGUAGE(3006, "errors.unsupported-programming-language", HttpStatus.BAD_REQUEST),

    LEARNING_PATH_NOT_FOUND(4000, "errors.learning-path-not-found", HttpStatus.NOT_FOUND),
    LEARNING_PATH_SLUG_ALREADY_EXISTS(4001, "errors.learning-path-slug-already-exists", HttpStatus.CONFLICT),
    TOPIC_NOT_FOUND(4002, "errors.topic.not-found", HttpStatus.NOT_FOUND),
    INVALID_LESSON_TYPE(4003, "errors.lesson-type.invalid", HttpStatus.BAD_REQUEST),
    LESSON_NOT_FOUND(4004, "errors.lesson-not-found", HttpStatus.NOT_FOUND),
    LESSON_SLUG_ALREADY_EXISTS(4005, "errors.lesson-slug-already-exists", HttpStatus.CONFLICT),

    INTERNAL_SERVER_ERROR(9999, "errors.server-error", HttpStatus.INTERNAL_SERVER_ERROR);

    private final int code;
    private final String key;
    private final HttpStatus httpStatus;

    ErrorCode(int code, String key, HttpStatus httpStatus) {
        this.code = code;
        this.key = key;
        this.httpStatus = httpStatus;
    }
}
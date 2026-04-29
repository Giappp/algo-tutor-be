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
    INVALID_CREDENTIALS(1000, "errors.invalid-credential", HttpStatus.NOT_FOUND),
    EMAIL_ALREADY_INUSE(1001, "errors.email-already-in-use", HttpStatus.BAD_REQUEST),
    USERNAME_TAKEN(1002, "errors.username-taken", HttpStatus.BAD_REQUEST),
    PASSWORD_MISMATCH(1003, "errors.password-mismatch", HttpStatus.NOT_FOUND),
    TOKEN_EXPIRED(1004, "errors.token-expired", HttpStatus.UNAUTHORIZED),
    INVALID_TOKEN(1005, "errors.invalid-token", HttpStatus.UNAUTHORIZED),
    USER_NOT_FOUND(1006, "errors.user-not-found", HttpStatus.NOT_FOUND),

    // Problem Errors
    PROBLEM_NOT_FOUND(2000, "errors.problem-not-found", HttpStatus.NOT_FOUND),
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
    TOPIC_NOT_FOUND(4002, "errors.topic-not-found", HttpStatus.NOT_FOUND),
    TOPIC_NOT_IN_LEARNING_PATH(4003, "errors.topic-not-in-learning-path", HttpStatus.BAD_REQUEST),
    INVALID_LESSON_TYPE(4004, "errors.lesson-type.invalid", HttpStatus.BAD_REQUEST),
    LESSON_NOT_FOUND(4005, "errors.lesson-not-found", HttpStatus.NOT_FOUND),
    LESSON_SLUG_ALREADY_EXISTS(4006, "errors.lesson-slug-already-exists", HttpStatus.CONFLICT),
    LESSON_NOT_IN_TOPIC(4007, "errors.lesson-not-in-topic", HttpStatus.BAD_REQUEST),
    TEST_CASE_NOT_FOUND(4008, "errors.test-case-not-found", HttpStatus.NOT_FOUND),
    TEST_CASE_NOT_IN_LESSON(4009, "errors.test-case-not-in-lesson", HttpStatus.BAD_REQUEST),
    QUIZ_QUESTION_NOT_FOUND(4010, "errors.quiz-question-not-found", HttpStatus.NOT_FOUND),
    QUIZ_CHOICE_NOT_FOUND(4011, "errors.quiz-choice-not-found", HttpStatus.NOT_FOUND),
    EDITORIAL_NOT_FOUND(4012, "errors.editorial-not-found", HttpStatus.NOT_FOUND),
    EDITORIAL_NOT_IN_LESSON(4013, "errors.editorial-not-in-lesson", HttpStatus.BAD_REQUEST),
    QUIZ_LESSON_REQUIRED(4014, "errors.quiz-lesson-required", HttpStatus.BAD_REQUEST),
    CODING_LESSON_REQUIRED(4015, "errors.coding-lesson-required", HttpStatus.BAD_REQUEST),

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
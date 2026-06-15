package org.rap.algotutorbe.common.errors;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    INVALID_PAYLOAD(1, "errors.invalid-payload", HttpStatus.BAD_REQUEST),
    ACCESS_DENIED(2, "errors.access-denied", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(3, "errors.need-authentication", HttpStatus.UNAUTHORIZED),
    CONFLICT_RESOURCE(4, "errors.conflict-resource", HttpStatus.CONFLICT),
    NOT_FOUND(5, "errors.not-found", HttpStatus.NOT_FOUND),
    FORBIDDEN(6, "errors.forbidden", HttpStatus.FORBIDDEN),
    INVALID_CREDENTIALS(1000, "errors.invalid-credential", HttpStatus.NOT_FOUND),
    EMAIL_ALREADY_INUSE(1001, "errors.email-already-in-use", HttpStatus.BAD_REQUEST),
    USERNAME_TAKEN(1002, "errors.username-taken", HttpStatus.BAD_REQUEST),
    PASSWORD_MISMATCH(1003, "errors.password-mismatch", HttpStatus.BAD_REQUEST),
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
    // Learning Path Errors
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
    INVALID_QUIZ_QUESTION(4016, "errors.quiz-choice-invalid", HttpStatus.INTERNAL_SERVER_ERROR),
    LESSON_LOCKED(4017, "errors.lesson-locked", HttpStatus.FORBIDDEN),
    VIDEO_LESSON_REQUIRED(4018, "errors.video-lesson-required", HttpStatus.BAD_REQUEST),
    VIDEO_INVALID_TYPE(4019, "errors.video-invalid-type", HttpStatus.BAD_REQUEST),
    VIDEO_INVALID_SIZE(4020, "errors.video-invalid-size", HttpStatus.BAD_REQUEST),
    VIDEO_UPLOAD_NOT_FOUND(4021, "errors.video-upload-not-found", HttpStatus.NOT_FOUND),
    VIDEO_UPLOAD_INCOMPLETE(4022, "errors.video-upload-incomplete", HttpStatus.BAD_REQUEST),
    VIDEO_NOT_READY(4023, "errors.video-not-ready", HttpStatus.CONFLICT),
    VIDEO_PROGRESS_INVALID(4024, "errors.video-progress-invalid", HttpStatus.BAD_REQUEST),
    VIDEO_PROGRESS_MANAGED_AUTOMATICALLY(4025, "errors.video-progress-managed-automatically", HttpStatus.BAD_REQUEST),

    // Enroll Errors
    ALREADY_ENROLL(5000, "errors.already-enroll", HttpStatus.CONFLICT),

    // Roadmap Errors
    LEARNING_PATH_NOT_PUBLISHED(6000, "errors.learning-path-not-published", HttpStatus.FORBIDDEN),
    ALREADY_ENROLLED(6001, "errors.already-enrolled", HttpStatus.CONFLICT),
    NOT_ENROLLED(6002, "errors.not-enrolled", HttpStatus.FORBIDDEN),
    TOPIC_LOCKED(6003, "errors.topic-locked", HttpStatus.FORBIDDEN),
    INVALID_PROGRESS_STATUS(6004, "errors.invalid-progress-status", HttpStatus.BAD_REQUEST),

    // Rate Limiting
    RATE_LIMITED(7000, "errors.rate-limited", HttpStatus.TOO_MANY_REQUESTS),

    // AI Chatbot Errors
    INVALID_CHAT_MODE(8000, "errors.ai.invalid-chat-mode", HttpStatus.BAD_REQUEST),
    CODE_REQUIRED(8001, "errors.ai.code-required", HttpStatus.BAD_REQUEST),
    CONVERSATION_NOT_FOUND(8002, "errors.ai.conversation-not-found", HttpStatus.NOT_FOUND),
    UNSUPPORTED_PROVIDER(8003, "errors.ai.unsupported-provider", HttpStatus.BAD_REQUEST),
    RATE_LIMIT_EXCEEDED(8004, "errors.ai.rate-limit-exceeded", HttpStatus.TOO_MANY_REQUESTS),
    AI_SERVICE_UNAVAILABLE(8005, "errors.ai.service-unavailable", HttpStatus.SERVICE_UNAVAILABLE),
    NO_MORE_HINTS(8006, "errors.ai.no-more-hints", HttpStatus.BAD_REQUEST),
    PROVIDER_NOT_CONFIGURED(8007, "errors.ai.provider-not-configured", HttpStatus.SERVICE_UNAVAILABLE),
    LOAD_TEMPLATE_FAILED(8008, "errors.ai.load-template-failed", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_AI_GENERATED_CONTENT(8009, "errors.ai.invalid-generated-content", HttpStatus.BAD_GATEWAY),
    INVALID_AI_QUESTION_SOURCES(8010, "errors.ai.invalid-question-sources", HttpStatus.BAD_REQUEST),
    INVALID_AI_GENERATED_QUESTIONS(8011, "errors.ai.invalid-generated-questions", HttpStatus.BAD_GATEWAY),
    INVALID_AI_CODING_SOURCES(8012, "errors.ai.invalid-coding-sources", HttpStatus.BAD_REQUEST),
    INVALID_AI_CODING_DRAFT(8013, "errors.ai.invalid-coding-draft", HttpStatus.BAD_GATEWAY),
    AI_GENERATED_CODE_COMPILE_FAILED(8014, "errors.ai.generated-code-compile-failed", HttpStatus.BAD_GATEWAY),

    FILE_EMPTY(9000, "errors.file.empty", HttpStatus.BAD_REQUEST),
    IMAGE_TYPE_ERROR(9001, "errors.image.type", HttpStatus.BAD_REQUEST),
    S3_CONNECTION(9002, "errors.s3.connection", HttpStatus.INTERNAL_SERVER_ERROR),
    S3_UNEXPECTED(9003, "errors.s3.unexpected", HttpStatus.INTERNAL_SERVER_ERROR),
    FILE_EXCEEDED_SIZE(9004, "errors.file.exceed.size", HttpStatus.BAD_REQUEST),
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

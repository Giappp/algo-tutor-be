package org.rap.algotutorbe.common.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rap.algotutorbe.common.api.ErrorResponse;
import org.rap.algotutorbe.common.errors.ErrorCode;
import org.rap.algotutorbe.common.exception.AppException;
import org.rap.algotutorbe.common.exception.RateLimitExceededException;
import org.rap.algotutorbe.judge.exception.JudgeConnectionException;
import org.rap.algotutorbe.judge.exception.PistonApiException;
import org.rap.algotutorbe.judge.exception.SolutionValidationException;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GeneralExceptionHandler {
    private final MessageSource messageSource;

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse<Object>> handleRateLimitExceeded(RateLimitExceededException ex) {
        String message = "Rate limit reached. Retry after " + ex.getRetryAfterSeconds() + " seconds";
        return ResponseEntity.status(ex.getError().getHttpStatus())
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getRetryAfterSeconds()))
                .body(ErrorResponse.buildError(message, ex.getError().getCode()));
    }

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse<Object>> handleAppException(AppException ex) {
        log.error(ex.getMessage(), ex);
        String message = resolveMessage(ex.getError());
        return ResponseEntity.status(ex.getError().getHttpStatus())
                .body(ErrorResponse.buildError(message, ex.getError().getCode()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse<Map<String, List<String>>>> handleValidation(MethodArgumentNotValidException ex) {
        Locale locale = LocaleContextHolder.getLocale();

        Map<String, List<String>> errors = getFieldErrors(ex, locale);

        return ResponseEntity.badRequest().body(ErrorResponse.buildError(errors, ErrorCode.INVALID_PAYLOAD.getCode()));
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ErrorResponse<Object>> handleAuthorizationDenied(
            AuthorizationDeniedException ex
    ) {
        log.error(ex.getMessage(), ex);

        String message = resolveMessage(ErrorCode.ACCESS_DENIED);

        return ResponseEntity.status(ErrorCode.ACCESS_DENIED.getHttpStatus())
                .body(ErrorResponse.buildError(
                        message,
                        ErrorCode.ACCESS_DENIED.getCode()
                ));
    }

    @ExceptionHandler(InsufficientAuthenticationException.class)
    public ResponseEntity<ErrorResponse<Object>> handleInsufficientAuthentication(
            InsufficientAuthenticationException ex
    ) {
        log.error(ex.getMessage(), ex);

        String message = resolveMessage(ErrorCode.UNAUTHORIZED);

        return ResponseEntity.status(ErrorCode.UNAUTHORIZED.getHttpStatus())
                .body(ErrorResponse.buildError(
                        message,
                        ErrorCode.UNAUTHORIZED.getCode()
                ));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse<Object>> handleAuthenticationException(
            AuthenticationException ex
    ) {
        log.error(ex.getMessage(), ex);

        String message = resolveMessage(ErrorCode.UNAUTHORIZED);

        return ResponseEntity.status(ErrorCode.UNAUTHORIZED.getHttpStatus())
                .body(ErrorResponse.buildError(
                        message,
                        ErrorCode.UNAUTHORIZED.getCode()
                ));
    }

    private Map<String, List<String>> getFieldErrors(MethodArgumentNotValidException ex, Locale locale) {
        Map<String, List<String>> errors = new LinkedHashMap<>();

        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            String message = resolveFieldError(fieldError, locale);
            errors.computeIfAbsent(fieldError.getField(), k -> new java.util.ArrayList<>()).add(message);
        }
        return errors;
    }

    private String resolveFieldError(FieldError fieldError, Locale locale) {
        return messageSource.getMessage(
                fieldError,
                locale
        );
    }

    private String resolveMessage(ErrorCode errorCode) {
        return messageSource.getMessage(
                errorCode.getKey(),
                null,
                LocaleContextHolder.getLocale()
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse<String>> handleHttpMessageNotReadable(HttpMessageNotReadableException exception) {
        log.error(exception.getMessage(), exception);
        String message = resolveMessage(ErrorCode.INVALID_PAYLOAD);
        return ResponseEntity.status(ErrorCode.INVALID_PAYLOAD.getHttpStatus())
                .body(ErrorResponse.buildError(message, ErrorCode.INVALID_PAYLOAD.getCode()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse<String>> handleMaxSizeException(MaxUploadSizeExceededException ex) {
        log.error(ex.getMessage(), ex);
        String message = resolveMessage(ErrorCode.FILE_EXCEEDED_SIZE);
        return ResponseEntity.status(ErrorCode.FILE_EXCEEDED_SIZE.getHttpStatus())
                .body(ErrorResponse.buildError(message, ErrorCode.FILE_EXCEEDED_SIZE.getCode()));
    }

    // Bắt lỗi từ Piston trả về (400, 404, 500 từ Piston)
    @ExceptionHandler(PistonApiException.class)
    public ResponseEntity<Object> handlePistonApiException(PistonApiException ex) {
        log.warn("Handled PistonApiException: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "EXECUTION_ENGINE_ERROR", ex.getMessage(), ex.getResponseBody());
    }

    @ExceptionHandler(JudgeConnectionException.class)
    public ResponseEntity<Object> handleJudgeConnectionException(JudgeConnectionException ex) {
        log.warn("Handled JudgeConnectionException: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.SERVICE_UNAVAILABLE, "JUDGE_SERVER_UNAVAILABLE", ex.getMessage(), null);
    }

    @ExceptionHandler(SolutionValidationException.class)
    public ResponseEntity<Object> handleSolutionValidationException(SolutionValidationException ex) {
        log.warn("Handled SolutionValidationException: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "TESTCASE_VALIDATION_FAILED", ex.getMessage(), ex.getDetails());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse<Object>> handleGlobalException(Exception ex) {
        log.error("Unhandled exception occurred", ex);
        String message = resolveMessage(ErrorCode.INTERNAL_SERVER_ERROR);
        return ResponseEntity.status(ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus())
                .body(ErrorResponse.buildError(message, ErrorCode.INTERNAL_SERVER_ERROR.getCode()));
    }

    private ResponseEntity<Object> buildErrorResponse(HttpStatus status, String errorCode, String message, Object details) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error_code", errorCode);
        body.put("message", message);
        if (details != null) {
            body.put("details", details);
        }
        return new ResponseEntity<>(body, status);
    }
}

package org.rap.algotutorbe.common.handler;

import lombok.RequiredArgsConstructor;
import org.rap.algotutorbe.common.api.ErrorResponse;
import org.rap.algotutorbe.common.errors.ErrorCode;
import org.rap.algotutorbe.common.exception.AppException;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestControllerAdvice
@RequiredArgsConstructor
public class GeneralExceptionHandler {
    private final MessageSource messageSource;

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse<Object>> handleAppException(AppException ex) {
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
        String message = resolveMessage(ErrorCode.INVALID_PAYLOAD);
        return ResponseEntity.status(ErrorCode.INVALID_PAYLOAD.getHttpStatus())
                .body(ErrorResponse.buildError(message, ErrorCode.INVALID_PAYLOAD.getCode()));
    }
}

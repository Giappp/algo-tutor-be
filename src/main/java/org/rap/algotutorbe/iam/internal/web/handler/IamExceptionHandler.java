package org.rap.algotutorbe.iam.internal.web.handler;

import org.rap.algotutorbe.common.api.ApiResponse;
import org.rap.algotutorbe.iam.internal.domain.exception.AuthException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class IamExceptionHandler {
    @ExceptionHandler(AuthException.class)
    public ResponseEntity<?> handleAuthException(AuthException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.builder()
                        .messages(exception.getMessage())
                        .code(exception.getErrorCode())
                        .success(false)
                        .build());
    }
}

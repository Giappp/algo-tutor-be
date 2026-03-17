package org.rap.algotutorbe.problem.infrastructure.handler;

import org.rap.algotutorbe.common.api.ApiResponse;
import org.rap.algotutorbe.problem.application.exception.ProblemNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ProblemExceptionHandler {

    @ExceptionHandler(ProblemNotFoundException.class)
    public ResponseEntity<?> handleProblemNotFound(ProblemNotFoundException exception) {
        return ResponseEntity.badRequest()
                .body(ApiResponse
                        .builder()
                        .messages(exception.getMessage())
                        .success(false)
                        .build());
    }
}

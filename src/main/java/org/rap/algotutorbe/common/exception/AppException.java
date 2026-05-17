package org.rap.algotutorbe.common.exception;

import lombok.Getter;
import org.rap.algotutorbe.common.errors.ErrorCode;

@Getter
public class AppException extends RuntimeException {
    private final ErrorCode error;

    public AppException(ErrorCode error) {
        super(error.getKey());
        this.error = error;
    }

    public AppException(ErrorCode error, Throwable cause) {
        super(error.getKey(), cause);
        this.error = error;
    }
}
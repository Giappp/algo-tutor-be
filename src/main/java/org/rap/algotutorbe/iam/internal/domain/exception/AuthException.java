package org.rap.algotutorbe.iam.internal.domain.exception;

import lombok.Getter;
import org.rap.algotutorbe.iam.internal.application.errors.ErrorCode;

@Getter
public class AuthException extends RuntimeException {
    private String errorCode;

    public AuthException(ErrorCode errorCode) {
        super(errorCode.message);
        this.errorCode = errorCode.code;
    }
}

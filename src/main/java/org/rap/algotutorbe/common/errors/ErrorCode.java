package org.rap.algotutorbe.common.errors;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    INVALID_PAYLOAD(1, "errors.invalid-payload", HttpStatus.BAD_REQUEST),
    INVALID_CREDENTIALS(1000, "Wrong username or password", HttpStatus.NOT_FOUND),
    EMAIL_ALREADY_INUSE(1001, "Email already used", HttpStatus.BAD_REQUEST),
    USERNAME_TAKEN(1002, "Username already taken", HttpStatus.BAD_REQUEST),
    PASSWORD_MISMATCH(1003, "Password and confirm password does not match", HttpStatus.NOT_FOUND),
    TOKEN_EXPIRED(1004, "Token expired", HttpStatus.UNAUTHORIZED),
    INVALID_TOKEN(1005, "Invalid Token or Already logout", HttpStatus.UNAUTHORIZED),
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
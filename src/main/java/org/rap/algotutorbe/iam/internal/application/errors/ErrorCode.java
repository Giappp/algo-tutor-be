package org.rap.algotutorbe.iam.internal.application.errors;

public enum ErrorCode {
    INVALID_CREDENTIALS("Wrong username or password", "1000"),
    EMAIL_ALREADY_INUSE("Email already used", "1001"),
    USERNAME_TAKEN("Username already taken", "1002"),
    PASSWORD_MISMATCH("Password and confirm password does not match", "1003"),
    TOKEN_EXPIRED("Token expired", "1004"),
    INVALID_TOKEN("Invalid Token or Already logout", "1005");
    public final String message;
    public final String code;

    ErrorCode(String message, String code) {
        this.message = message;
        this.code = code;
    }
}
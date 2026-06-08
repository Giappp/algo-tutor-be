package org.rap.algotutorbe.iam.infrastructure.config;

public class Messages {
    public static final String INVALID_BODY = "Invalid request body";

    public static class Validation {
        public static final String PASSWORD = "Password must be min 8 and max 16 length, containing at least 1 uppercase, 1 lowercase, 1 special character, and 1 digit";
        public static final String EMAIL = "Email not valid";
    }
}

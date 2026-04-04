package org.rap.algotutorbe.judge.domain.exception;

import lombok.Getter;

@Getter
public class PistonApiException extends JudgeException {
    private final int statusCode;
    private final String responseBody;

    public PistonApiException(String message, int statusCode, String responseBody) {
        super(message);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

}

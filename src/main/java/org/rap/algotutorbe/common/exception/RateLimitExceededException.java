package org.rap.algotutorbe.common.exception;

import lombok.Getter;
import org.rap.algotutorbe.common.errors.ErrorCode;

@Getter
public class RateLimitExceededException extends AppException {
    private final long retryAfterSeconds;

    public RateLimitExceededException(long retryAfterSeconds) {
        super(ErrorCode.RATE_LIMIT_EXCEEDED);
        this.retryAfterSeconds = retryAfterSeconds;
    }
}

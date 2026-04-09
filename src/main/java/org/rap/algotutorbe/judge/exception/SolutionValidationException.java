package org.rap.algotutorbe.judge.exception;

import lombok.Getter;
import org.rap.algotutorbe.judge.dto.ValidationDetail;

import java.util.List;

@Getter
public class SolutionValidationException extends RuntimeException {
    private final List<ValidationDetail> details;

    public SolutionValidationException(String message, List<ValidationDetail> details) {
        super(message);
        this.details = details;
    }
}

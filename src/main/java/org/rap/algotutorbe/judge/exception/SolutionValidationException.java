package org.rap.algotutorbe.judge.exception;

import lombok.Getter;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Getter
public class SolutionValidationException extends RuntimeException implements Serializable {
    private final List<Map<String, Object>> details;

    public SolutionValidationException(String message, List<Map<String, Object>> details) {
        super(message);
        this.details = details;
    }
}

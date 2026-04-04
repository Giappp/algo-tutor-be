package org.rap.algotutorbe.problem.application.exception;

import lombok.Getter;

@Getter
public class TestcasesNotFoundException extends RuntimeException {
    public TestcasesNotFoundException(String message) {
        super(message);
    }
}

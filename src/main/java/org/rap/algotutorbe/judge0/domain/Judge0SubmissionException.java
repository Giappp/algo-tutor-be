package org.rap.algotutorbe.judge0.domain;

import lombok.Getter;

@Getter
public class Judge0SubmissionException extends RuntimeException {
    public Judge0SubmissionException(String message) {
        super(message);
    }
}

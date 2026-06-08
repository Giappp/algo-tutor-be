package org.rap.algotutorbe.learning.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum EnrollmentStatus {
    IN_PROGRESS("ACTIVE"),
    COMPLETED("COMPLETED"),
    DROPPED("DROPPED");

    private final String apiValue;

    EnrollmentStatus(String apiValue) {
        this.apiValue = apiValue;
    }

    @JsonValue
    public String getApiValue() {
        return apiValue;
    }
}

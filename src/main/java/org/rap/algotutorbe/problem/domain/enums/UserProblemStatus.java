package org.rap.algotutorbe.problem.domain.enums;

public enum UserProblemStatus {
    NOT_STARTED("not-started"),
    ATTEMPTED("attempted"),
    SOLVED("solved");

    private final String apiValue;

    UserProblemStatus(String apiValue) {
        this.apiValue = apiValue;
    }

    public String toApiValue() {
        return apiValue;
    }

    public static UserProblemStatus fromApiValue(String value) {
        if (value == null) return null;
        return switch (value) {
            case "not-started" -> NOT_STARTED;
            case "attempted" -> ATTEMPTED;
            case "solved" -> SOLVED;
            default -> throw new IllegalArgumentException("Unsupported status: " + value);
        };
    }
}


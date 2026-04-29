package org.rap.algotutorbe.learning.enums;

public enum Difficulty {
    EASY,
    MEDIUM,
    HARD;

    public static Difficulty fromApiValue(String value) {
        if (value == null) return null;
        return switch (value) {
            case "Easy" -> EASY;
            case "Medium" -> MEDIUM;
            case "Hard" -> HARD;
            default -> Difficulty.valueOf(value.toUpperCase());
        };
    }

    public String toApiValue() {
        return switch (this) {
            case EASY -> "Easy";
            case MEDIUM -> "Medium";
            case HARD -> "Hard";
        };
    }
}

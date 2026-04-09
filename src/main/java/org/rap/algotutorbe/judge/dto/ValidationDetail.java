package org.rap.algotutorbe.judge.dto;

public record ValidationDetail(
        int testcaseIndex,
        String status,
        String input,
        String expectedOutput,
        String actualOutput,
        String errorMessage
) {
}
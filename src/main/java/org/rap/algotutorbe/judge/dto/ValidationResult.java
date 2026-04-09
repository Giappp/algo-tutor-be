package org.rap.algotutorbe.judge.dto;

import lombok.Getter;

import java.util.List;

public record ValidationResult(
        boolean isAllPassed,
        @Getter
        List<ValidationDetail> details
) {
}
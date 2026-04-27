package org.rap.algotutorbe.judge.dto;

import java.util.List;

public record ValidationResult(
        boolean isAllPassed,
        List<ValidationDetail> details
) {
}

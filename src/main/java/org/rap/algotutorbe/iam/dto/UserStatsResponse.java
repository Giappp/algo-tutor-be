package org.rap.algotutorbe.iam.dto;

import java.math.BigDecimal;

public record UserStatsResponse(
        long totalSolved,
        long easySolved,
        long mediumSolved,
        long hardSolved,
        long totalSubmissions,
        BigDecimal acceptanceRate
) {
}

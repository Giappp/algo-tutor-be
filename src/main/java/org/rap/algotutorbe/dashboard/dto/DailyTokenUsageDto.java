package org.rap.algotutorbe.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DailyTokenUsageDto {
    private LocalDate date;
    private long inputTokens;
    private long outputTokens;
    private long totalTokens;
}

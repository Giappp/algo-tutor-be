package org.rap.algotutorbe.dashboard.dto;

import lombok.Builder;
import lombok.Getter;
import java.util.List;
import java.util.Map;

@Getter
@Builder
public class AiTokenUsageDto {
    private long totalInputTokens;
    private long totalOutputTokens;
    private long totalTokensCombined;
    private List<DailyTokenUsageDto> dailyUsage;
    private Map<String, Long> usageByMode;
    private List<UserTokenUsageDto> topConsumers;
}

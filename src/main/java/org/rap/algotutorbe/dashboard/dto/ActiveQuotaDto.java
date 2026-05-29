package org.rap.algotutorbe.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActiveQuotaDto {
    private String key;
    private String action;
    private String userId;
    private String username;
    private String email;
    private int currentRequests;
    private int maxLimit;
    private long windowSeconds;
    private long oldestTimestampMs;
}

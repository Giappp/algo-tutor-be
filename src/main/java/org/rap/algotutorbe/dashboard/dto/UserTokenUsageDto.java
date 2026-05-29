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
public class UserTokenUsageDto {
    private String userId;
    private String username;
    private String email;
    private long inputTokens;
    private long outputTokens;
    private long totalTokens;
}

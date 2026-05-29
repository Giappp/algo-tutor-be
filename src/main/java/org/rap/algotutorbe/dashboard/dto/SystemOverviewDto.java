package org.rap.algotutorbe.dashboard.dto;

import lombok.Builder;
import lombok.Getter;
import java.util.Map;

@Getter
@Builder
public class SystemOverviewDto {
    private long totalUsers;
    private long activeSessions;
    private long totalLessons;
    private long totalEnrollments;
    private long totalSubmissions;
    private long totalQuizAttempts;
    private Map<String, Long> verdictDistribution;
    private Map<String, Long> lessonDistribution;
}

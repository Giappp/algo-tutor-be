package org.rap.algotutorbe.dashboard.service;

import lombok.RequiredArgsConstructor;
import org.rap.algotutorbe.ai.repository.AiMessageRepository;
import org.rap.algotutorbe.common.ratelimit.RateLimiter;
import org.rap.algotutorbe.dashboard.dto.*;
import org.rap.algotutorbe.iam.domain.model.User;
import org.rap.algotutorbe.iam.domain.repositories.RefreshTokenRepository;
import org.rap.algotutorbe.iam.domain.repositories.UserRepository;
import org.rap.algotutorbe.learning.repositories.EnrollmentRepository;
import org.rap.algotutorbe.learning.repositories.LessonRepository;
import org.rap.algotutorbe.learning.repositories.QuizAttemptRepository;
import org.rap.algotutorbe.submission.repositories.SubmissionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashboardService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final LessonRepository lessonRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final SubmissionRepository submissionRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final AiMessageRepository aiMessageRepository;
    private final RateLimiter rateLimiter;

    @Value("${ai.rate-limit.max-requests:20}")
    private int maxRequests;

    @Value("${ai.rate-limit.window-seconds:60}")
    private int windowSeconds;

    /**
     * Get system overview metrics including general counts and status distributions.
     */
    public SystemOverviewDto getSystemOverview() {
        long totalUsers = userRepository.count();
        long activeSessions = refreshTokenRepository.count();
        long totalLessons = lessonRepository.count();
        long totalEnrollments = enrollmentRepository.count();
        long totalSubmissions = submissionRepository.count();
        long totalQuizAttempts = quizAttemptRepository.count();

        // Get submission verdict distribution
        List<Object[]> verdictData = submissionRepository.getVerdictDistribution();
        Map<String, Long> verdictDistribution = verdictData.stream()
                .collect(Collectors.toMap(
                        row -> row[0] != null ? row[0].toString() : "UNKNOWN",
                        row -> ((Number) row[1]).longValue(),
                        Long::sum
                ));

        // Get lesson type distribution
        List<Object[]> lessonData = lessonRepository.getLessonDistribution();
        Map<String, Long> lessonDistribution = lessonData.stream()
                .collect(Collectors.toMap(
                        row -> row[0] != null ? row[0].toString() : "UNKNOWN",
                        row -> ((Number) row[1]).longValue(),
                        Long::sum
                ));

        return SystemOverviewDto.builder()
                .totalUsers(totalUsers)
                .activeSessions(activeSessions)
                .totalLessons(totalLessons)
                .totalEnrollments(totalEnrollments)
                .totalSubmissions(totalSubmissions)
                .totalQuizAttempts(totalQuizAttempts)
                .verdictDistribution(verdictDistribution)
                .lessonDistribution(lessonDistribution)
                .build();
    }

    /**
     * Get AI token consumption statistics.
     */
    public AiTokenUsageDto getAiTokenUsage(int daysLimit) {
        Long totalInput = aiMessageRepository.sumInputTokens();
        Long totalOutput = aiMessageRepository.sumOutputTokens();
        long totalInputTokens = totalInput != null ? totalInput : 0L;
        long totalOutputTokens = totalOutput != null ? totalOutput : 0L;
        long totalCombined = totalInputTokens + totalOutputTokens;

        // Daily usage
        Instant since = Instant.now().minus(daysLimit, ChronoUnit.DAYS);
        List<Object[]> dailyData = aiMessageRepository.getDailyTokenUsage(since);
        List<DailyTokenUsageDto> dailyUsage = dailyData.stream().map(row -> {
            LocalDate date = null;
            if (row[0] instanceof java.sql.Date) {
                date = ((java.sql.Date) row[0]).toLocalDate();
            } else if (row[0] instanceof LocalDate) {
                date = (LocalDate) row[0];
            } else if (row[0] instanceof Date) {
                date = new java.sql.Date(((Date) row[0]).getTime()).toLocalDate();
            }
            long input = row[1] != null ? ((Number) row[1]).longValue() : 0L;
            long output = row[2] != null ? ((Number) row[2]).longValue() : 0L;
            return new DailyTokenUsageDto(date, input, output, input + output);
        }).collect(Collectors.toList());

        // Usage by Mode
        List<Object[]> modeData = aiMessageRepository.getTokenUsageByMode();
        Map<String, Long> usageByMode = modeData.stream().collect(Collectors.toMap(
                row -> row[0] != null ? row[0].toString() : "UNKNOWN",
                row -> {
                    long input = row[1] != null ? ((Number) row[1]).longValue() : 0L;
                    long output = row[2] != null ? ((Number) row[2]).longValue() : 0L;
                    return input + output;
                },
                Long::sum
        ));

        // TopConsumers
        List<Object[]> consumersData = aiMessageRepository.getTopTokenConsumers(10);
        List<UserTokenUsageDto> topConsumers = consumersData.stream().map(row -> {
            String userId = row[0] != null ? row[0].toString() : null;
            String username = row[1] != null ? row[1].toString() : "UNKNOWN";
            String email = row[2] != null ? row[2].toString() : "UNKNOWN";
            long input = row[3] != null ? ((Number) row[3]).longValue() : 0L;
            long output = row[4] != null ? ((Number) row[4]).longValue() : 0L;
            return UserTokenUsageDto.builder()
                    .userId(userId)
                    .username(username)
                    .email(email)
                    .inputTokens(input)
                    .outputTokens(output)
                    .totalTokens(input + output)
                    .build();
        }).collect(Collectors.toList());

        return AiTokenUsageDto.builder()
                .totalInputTokens(totalInputTokens)
                .totalOutputTokens(totalOutputTokens)
                .totalTokensCombined(totalCombined)
                .dailyUsage(dailyUsage)
                .usageByMode(usageByMode)
                .topConsumers(topConsumers)
                .build();
    }

    /**
     * Get active API Quota & Rate Limit status for all current sliding window requests.
     */
    public List<ActiveQuotaDto> getActiveApiQuotas() {
        Map<String, Deque<Long>> rawLog = rateLimiter.getRequestLog();
        if (rawLog.isEmpty()) {
            return Collections.emptyList();
        }

        long now = System.currentTimeMillis();
        List<ActiveQuotaDto> list = new ArrayList<>();

        // Group unique user IDs to do a single batch query
        List<UUID> userIds = rawLog.keySet().stream()
                .map(this::extractUserId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        Map<UUID, User> userMap = Collections.emptyMap();
        if (!userIds.isEmpty()) {
            userMap = userRepository.findAllById(userIds).stream()
                    .collect(Collectors.toMap(User::getId, Function.identity()));
        }

        for (Map.Entry<String, Deque<Long>> entry : rawLog.entrySet()) {
            String key = entry.getKey();
            Deque<Long> timestamps = entry.getValue();

            long windowMs = determineWindowSeconds(key) * 1000L;
            long cutoff = now - windowMs;

            // Count timestamps within the active window only
            List<Long> activeTimestamps = timestamps.stream()
                    .filter(t -> t >= cutoff)
                    .collect(Collectors.toList());

            if (activeTimestamps.isEmpty()) {
                continue;
            }

            UUID userId = extractUserId(key);
            User user = userMap.get(userId);

            String username = user != null ? user.getUsername() : "UNKNOWN";
            String email = user != null ? user.getEmail() : "UNKNOWN";
            Long oldestTimestamp = activeTimestamps.get(0);

            list.add(ActiveQuotaDto.builder()
                    .key(key)
                    .action(determineAction(key))
                    .userId(userId != null ? userId.toString() : null)
                    .username(username)
                    .email(email)
                    .currentRequests(activeTimestamps.size())
                    .maxLimit(determineMaxLimit(key))
                    .windowSeconds(windowMs / 1000L)
                    .oldestTimestampMs(oldestTimestamp)
                    .build());
        }

        // Sort by highest quota usage (most requests first)
        list.sort((a, b) -> Integer.compare(b.getCurrentRequests(), a.getCurrentRequests()));
        return list;
    }

    private UUID extractUserId(String key) {
        try {
            if (key.startsWith("ai-chat:")) {
                return UUID.fromString(key.substring("ai-chat:".length()));
            } else if (key.startsWith("ai-general-chat:")) {
                return UUID.fromString(key.substring("ai-general-chat:".length()));
            } else if (key.startsWith("judge:run:")) {
                return UUID.fromString(key.substring("judge:run:".length()));
            } else if (key.startsWith("judge:submit:")) {
                return UUID.fromString(key.substring("judge:submit:".length()));
            }
        } catch (Exception e) {
            // Silently ignore parsing errors
        }
        return null;
    }

    private String determineAction(String key) {
        if (key.startsWith("ai-chat:")) return "AI Chat";
        if (key.startsWith("ai-general-chat:")) return "AI Advisor / General Chat";
        if (key.startsWith("judge:run:")) return "Run Code";
        if (key.startsWith("judge:submit:")) return "Submit Code";
        return "Unknown";
    }

    private int determineMaxLimit(String key) {
        if (key.startsWith("ai-chat:") || key.startsWith("ai-general-chat:")) {
            return maxRequests;
        }
        if (key.startsWith("judge:run:")) {
            return 10;
        }
        if (key.startsWith("judge:submit:")) {
            return 5;
        }
        return 0;
    }

    private long determineWindowSeconds(String key) {
        if (key.startsWith("ai-chat:") || key.startsWith("ai-general-chat:")) {
            return windowSeconds;
        }
        if (key.startsWith("judge:run:") || key.startsWith("judge:submit:")) {
            return 60;
        }
        return 0;
    }
}

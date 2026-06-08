package org.rap.algotutorbe.common.ratelimit;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodic cleanup task for the rate limiter to prevent memory leaks.
 */
@Component
@RequiredArgsConstructor
public class RateLimitCleanupTask {

    private final RateLimiter rateLimiter;

    @Scheduled(fixedRate = 60_000) // Every 60 seconds
    public void cleanup() {
        rateLimiter.cleanup();
    }
}

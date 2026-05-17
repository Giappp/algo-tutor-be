package org.rap.algotutorbe.common.ratelimit;

import org.springframework.stereotype.Component;

import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Simple in-memory sliding window rate limiter.
 * Tracks request timestamps per user and checks against configured limits.
 */
@Component
public class RateLimiter {

    private final ConcurrentHashMap<String, Deque<Long>> requestLog = new ConcurrentHashMap<>();

    /**
     * Check if the request is allowed under the rate limit.
     *
     * @param key          unique key (e.g., "run:userId" or "submit:userId")
     * @param maxRequests  maximum number of requests allowed in the window
     * @param windowMillis time window in milliseconds
     * @return true if allowed, false if rate limited
     */
    public boolean isAllowed(String key, int maxRequests, long windowMillis) {
        long now = System.currentTimeMillis();
        long windowStart = now - windowMillis;

        Deque<Long> timestamps = requestLog.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>());

        // Remove expired entries
        while (!timestamps.isEmpty() && timestamps.peekFirst() < windowStart) {
            timestamps.pollFirst();
        }

        if (timestamps.size() >= maxRequests) {
            return false;
        }

        timestamps.addLast(now);
        return true;
    }

    /**
     * Clean up expired entries to prevent memory leaks.
     * Should be called periodically (e.g., via @Scheduled).
     */
    public void cleanup() {
        long now = System.currentTimeMillis();
        // Remove entries older than 2 minutes (generous window)
        long cutoff = now - 120_000;

        requestLog.entrySet().removeIf(entry -> {
            Deque<Long> timestamps = entry.getValue();
            while (!timestamps.isEmpty() && timestamps.peekFirst() < cutoff) {
                timestamps.pollFirst();
            }
            return timestamps.isEmpty();
        });
    }
}

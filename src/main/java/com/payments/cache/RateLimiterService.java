package com.payments.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Redis-backed sliding window rate limiter.
 * <br/>
 * Uses Redis INCR + EXPIRE to count requests per key within a time window.<br/>
 * Applied at the payment processing layer to prevent abuse.
 * <br/>
 * Strategy:
 *   Key format: rate_limit:{scope}:{identifier}
 *   On each call: INCR the key,<br/> set TTL on first increment.
 *   If count exceeds limit → reject with RateLimitExceededException.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimiterService {

    private final RedisTemplate<String, Object> redisTemplate;

    // Default limits
    private static final int  MAX_REQUESTS_PER_MINUTE     = 10;
    private static final int  MAX_REQUESTS_PER_HOUR       = 100;
    private static final long WINDOW_SECONDS_MINUTE       = 60L;
    private static final long WINDOW_SECONDS_HOUR         = 3600L;

    /**
     * Check and increment rate limit counters for a given identifier.<br/>
     * Throws if either the per-minute or per-hour limit is exceeded.
     *
     * @param identifier usually the client IP or recipient identifier
     */
    public void checkRateLimit(String identifier) {
        checkLimit("minute", identifier, MAX_REQUESTS_PER_MINUTE, WINDOW_SECONDS_MINUTE);
        checkLimit("hour",   identifier, MAX_REQUESTS_PER_HOUR,   WINDOW_SECONDS_HOUR);
    }

    /**
     * Retrieve remaining requests in the current minute window.
     */
    public int getRemainingRequests(String identifier) {
        String key = buildKey("minute", identifier);
        Object count = redisTemplate.opsForValue().get(key);
        if (count == null) return MAX_REQUESTS_PER_MINUTE;
        int used = Integer.parseInt(count.toString());
        return Math.max(0, MAX_REQUESTS_PER_MINUTE - used);
    }

    /**
     * Retrieve the TTL (seconds) remaining for the minute window.
     */
    public long getWindowTtl(String identifier) {
        String key = buildKey("minute", identifier);
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        return ttl != null && ttl > 0 ? ttl : WINDOW_SECONDS_MINUTE;
    }

    private void checkLimit(String scope, String identifier, int maxRequests, long windowSeconds) {
        String key = buildKey(scope, identifier);
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1) {
                // First request in this window — set the expiry
                redisTemplate.expire(key, Duration.ofSeconds(windowSeconds));
            }
            if (count != null && count > maxRequests) {
                log.warn("Rate limit exceeded [{}/{}] for key: {}", count, maxRequests, key);
                throw new RateLimitExceededException(
                    String.format("Rate limit exceeded: %d/%d requests in the last %s. Please slow down.",
                        count, maxRequests, scope)
                );
            }
            log.debug("Rate check [{}]: {}/{} for {}", scope, count, maxRequests, identifier);
        } catch (RateLimitExceededException ex) {
            throw ex;
        } catch (Exception ex) {
            // Redis unavailable — fail open (don't block payments)
            log.error("Redis rate limiter error for key {}. Failing open.", key, ex);
        }
    }

    private String buildKey(String scope, String identifier) {
        return "rate_limit:" + scope + ":" + identifier;
    }

    /** Typed exception for rate limit violations */
    public static class RateLimitExceededException extends RuntimeException {
        public RateLimitExceededException(String message) {
            super(message);
        }
    }
}

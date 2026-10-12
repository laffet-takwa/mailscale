package com.mailscale.email;

import com.mailscale.common.RateLimitExceededException;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Sliding-window rate limiter backed by Redis.
 *
 * <p>Uses a sorted set with timestamps as scores: adds the
 * request timestamp, drops entries older than the window, and
 * counts the remaining entries. Complexity is O(log n) per
 * request for the add and O(log n) for the trim via
 * {@code ZREMRANGEBYSCORE}.
 *
 * <p>When Redis is not configured (tests, local profile) the
 * limiter degrades to a no-op so the API stays usable; this is
 * documented as a limitation of the test profile.
 */
@Service
public class RateLimitService {

    private static final Logger log = LoggerFactory.getLogger(
            RateLimitService.class);

    private static final long WINDOW_MINUTES = 1;

    private final StringRedisTemplate redisTemplate;
    private final int maxPerMinute;

    public RateLimitService(
            ObjectProvider<StringRedisTemplate> redisProvider,
            @Value("${mailscale.mail.rate-limit-per-minute:60}") int maxPerMinute) {
        this.redisTemplate = redisProvider.getIfAvailable();
        this.maxPerMinute = maxPerMinute;
    }

    /**
     * @throws RateLimitExceededException when the limit is exceeded
     */
    public void check(String key) {
        if (redisTemplate == null) {
            return;
        }
        String redisKey = "ratelimit:" + key;
        long now = Instant.now().toEpochMilli();
        long windowStart = now
                - TimeUnit.MINUTES.toMillis(WINDOW_MINUTES);

        redisTemplate.opsForZSet().removeRangeByScore(redisKey,
                0, windowStart);
        Long count = redisTemplate.opsForZSet().zCard(redisKey);
        if (count != null && count >= maxPerMinute) {
            log.warn("Rate limit exceeded for key {}: {}/{} per minute",
                    key, count, maxPerMinute);
            throw new RateLimitExceededException(
                    "Rate limit exceeded: " + maxPerMinute
                            + " requests per minute");
        }
        redisTemplate.opsForZSet().add(redisKey,
                String.valueOf(now), now);
        redisTemplate.expire(redisKey, WINDOW_MINUTES,
                TimeUnit.MINUTES);
    }
}

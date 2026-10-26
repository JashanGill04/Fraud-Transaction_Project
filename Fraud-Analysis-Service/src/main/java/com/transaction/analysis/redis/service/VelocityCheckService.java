package com.transaction.analysis.redis.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class VelocityCheckService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final int MAX_TRANSACTIONS = 3;
    private static final Duration WINDOW = Duration.ofSeconds(60);

    /** What the velocity check saw, so the pipeline can explain its decision. */
    public record Result(long count, int maxAllowed, long windowSeconds, boolean exceeded) {}

    /**
     * Counts this transaction against the card's current time window.
     */
    public Result check(String cardNumberHash) {
        String key = "velocity:" + cardNumberHash;

        Long count = redisTemplate.opsForValue().increment(key);

        if (count != null && count == 1) {
            // first transaction in this window: start the expiry clock
            redisTemplate.expire(key, WINDOW);
        }

        long c = count == null ? 0 : count;
        log.debug("Velocity check for card hash={}, count={} in current window", cardNumberHash, c);

        return new Result(c, MAX_TRANSACTIONS, WINDOW.toSeconds(), c > MAX_TRANSACTIONS);
    }
}
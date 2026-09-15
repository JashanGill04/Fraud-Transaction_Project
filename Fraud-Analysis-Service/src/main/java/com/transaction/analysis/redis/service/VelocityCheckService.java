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

    /**
     * Increments the transaction count for this card within the current time window.
     * Returns true if this transaction exceeds the velocity threshold.
     */
    public boolean isVelocityExceeded(String cardNumberHash) {
        String key = "velocity:" + cardNumberHash;

        Long count = redisTemplate.opsForValue().increment(key);

        if (count != null && count == 1) {
            // first transaction in this window — set the expiry
            redisTemplate.expire(key, WINDOW);
        }

        log.debug("Velocity check for card hash={}, count={} in current window", cardNumberHash, count);

        return count != null && count > MAX_TRANSACTIONS;
    }
}
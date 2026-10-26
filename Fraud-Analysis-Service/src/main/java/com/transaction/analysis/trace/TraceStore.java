package com.transaction.analysis.trace;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.transaction.analysis.kafka.event.TransactionCreatedEvent;
import com.transaction.analysis.model.DecisionTrace;
import com.transaction.analysis.model.FraudDecision;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Stores each transaction's decision trace as a JSON string in Redis.
 * We keep plain JSON strings (not objects) so reading them back never hits
 * the "LinkedHashMap cannot be cast" problem again.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TraceStore {

    private static final String KEY_PREFIX = "trace:";
    private static final String RECENT_KEY = "traces:recent";
    private static final int MAX_RECENT = 200;
    private static final Duration TTL = Duration.ofDays(7);

    private final StringRedisTemplate redis;

    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    /** Call this right after analyze(), before anything is published to Kafka. */
    public void record(TransactionCreatedEvent event, FraudDecision decision) {
        DecisionTrace trace = DecisionTrace.builder()
                .transactionId(event.getTransactionId())
                .maskedCardNumber(event.getMaskedCardNumber())
                .amount(event.getAmount())
                .currency(event.getCurrency())
                .merchantId(event.getMerchantId())
                .latitude(event.getLatitude())
                .longitude(event.getLongitude())
                .transactionTime(event.getTimestamp())
                .decidedAt(Instant.now())
                .verdict(decision.isFraudulent() ? "FLAGGED" : "APPROVED")
                .reason(decision.getReason())
                .durationMs(decision.getDurationMs())
                .steps(decision.getSteps())
                .build();
        save(trace);
    }

    private void save(DecisionTrace trace) {
        try {
            String id = trace.getTransactionId().toString();
            redis.opsForValue().set(KEY_PREFIX + id, mapper.writeValueAsString(trace), TTL);
            redis.opsForList().remove(RECENT_KEY, 0, id);   // no duplicates if Kafka redelivers
            redis.opsForList().leftPush(RECENT_KEY, id);
            redis.opsForList().trim(RECENT_KEY, 0, MAX_RECENT - 1);
        } catch (Exception ex) {
            // The audit trail is a bonus; it must never break fraud detection itself
            log.warn("Could not save decision trace for txnId={}", trace.getTransactionId(), ex);
        }
    }

    /** Newest first, as a JSON array string. */
    public String listJson(int limit) {
        List<String> ids = redis.opsForList().range(RECENT_KEY, 0, Math.max(limit, 1) - 1);
        if (ids == null || ids.isEmpty()) {
            return "[]";
        }
        List<String> keys = ids.stream().map(id -> KEY_PREFIX + id).toList();
        List<String> values = redis.opsForValue().multiGet(keys);
        if (values == null) {
            return "[]";
        }
        return values.stream().filter(Objects::nonNull).collect(Collectors.joining(",", "[", "]"));
    }

    public Optional<String> getJson(String transactionId) {
        return Optional.ofNullable(redis.opsForValue().get(KEY_PREFIX + transactionId));
    }
}
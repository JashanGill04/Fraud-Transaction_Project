package com.transaction.analysis.service;

import com.transaction.analysis.ai.service.AnomalyDetectionService;
import com.transaction.analysis.kafka.event.TransactionCreatedEvent;
import com.transaction.analysis.model.FraudDecision;
import com.transaction.analysis.model.TraceStep;
import com.transaction.analysis.redis.service.GeolocationCheckService;
import com.transaction.analysis.redis.service.VelocityCheckService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class FraudAnalysisService {

    private final VelocityCheckService velocityCheckService;
    private final GeolocationCheckService geolocationCheckService;
    private final AnomalyDetectionService anomalyDetectionService;

    public FraudDecision analyze(TransactionCreatedEvent event) {
        long start = System.currentTimeMillis();
        List<TraceStep> steps = new ArrayList<>();

        // 1. Velocity rule (cheap)
        VelocityCheckService.Result v = velocityCheckService.check(event.getCardNumberHash());
        steps.add(step("VELOCITY", "Velocity check",
                v.exceeded() ? "FAIL" : "PASS",
                v.exceeded()
                        ? "Swipe #%d on this card within %d seconds (limit is %d)"
                        .formatted(v.count(), v.windowSeconds(), v.maxAllowed())
                        : "Swipe #%d in the last %d seconds (limit is %d)"
                        .formatted(v.count(), v.windowSeconds(), v.maxAllowed()),
                details("Swipes in window", v.count(),
                        "Allowed per window", v.maxAllowed(),
                        "Window (seconds)", v.windowSeconds())));

        if (v.exceeded()) {
            log.info("Transaction flagged: VELOCITY_EXCEEDED, txnId={}", event.getTransactionId());
            steps.add(skipped("GEOLOCATION", "Geolocation check", "Not run because the velocity check already failed"));
            steps.add(skipped("AI", "AI anomaly check", "Not run because the velocity check already failed"));
            return FraudDecision.flagged("VELOCITY_EXCEEDED", null, steps, elapsed(start));
        }

        // 2. Geolocation rule (cheap)
        GeolocationCheckService.Result g = geolocationCheckService.check(
                event.getCardNumberHash(), event.getLatitude(), event.getLongitude(), event.getTimestamp());

        if (!g.compared()) {
            steps.add(step("GEOLOCATION", "Geolocation check", "PASS",
                    "First location seen for this card, so there is nothing to compare against",
                    details("Previous location", "none in the last 24 hours")));
        } else {
            String moved = "%s km in %s".formatted(
                    String.format(Locale.US, "%,.0f", g.distanceKm()), humanDuration(g.hoursElapsed()));
            steps.add(step("GEOLOCATION", "Geolocation check",
                    g.impossible() ? "FAIL" : "PASS",
                    g.impossible()
                            ? "Moved %s, which implies %s km/h (limit is %s km/h)".formatted(moved,
                            String.format(Locale.US, "%,.0f", g.speedKmh()),
                            String.format(Locale.US, "%,.0f", g.maxSpeedKmh()))
                            : "Moved %s, which is plausible (%s km/h)".formatted(moved,
                            String.format(Locale.US, "%,.0f", g.speedKmh())),
                    details("Distance from last swipe (km)", Math.round(g.distanceKm() * 10) / 10.0,
                            "Time since last swipe", humanDuration(g.hoursElapsed()),
                            "Implied speed (km/h)", Math.round(g.speedKmh()),
                            "Speed limit (km/h)", Math.round(g.maxSpeedKmh()))));
        }

        if (g.impossible()) {
            log.info("Transaction flagged: IMPOSSIBLE_TRAVEL, txnId={}", event.getTransactionId());
            steps.add(skipped("AI", "AI anomaly check", "Not run because the geolocation check already failed"));
            return FraudDecision.flagged("IMPOSSIBLE_TRAVEL", null, steps, elapsed(start));
        }

        // 3. AI check (slow), only for transactions the rules did not catch
        try {
            AnomalyDetectionService.Result a = anomalyDetectionService.analyze(event);
            steps.add(step("AI", "AI anomaly check", a.flagged() ? "FAIL" : "PASS",
                    a.flagged() ? "The model judged this transaction as fraud"
                            : "The model judged this transaction as legitimate",
                    details("Search text", a.query(),
                            "Knowledge retrieved", a.retrievedContext(),
                            "Model response", a.response())));

            if (a.flagged()) {
                log.info("Transaction flagged: AI_ANOMALY, txnId={}", event.getTransactionId());
                return FraudDecision.flagged("AI_ANOMALY", null, steps, elapsed(start));
            }
        } catch (Exception ex) {
            // Fail open on purpose: rules already passed. Change this if you prefer fail closed.
            log.warn("AI check failed for txnId={}", event.getTransactionId(), ex);
            steps.add(step("AI", "AI anomaly check", "ERROR",
                    "The AI check failed, so this decision was made by the rules only",
                    details("Error", ex.getClass().getSimpleName() + ": " + ex.getMessage())));
        }

        log.debug("Transaction clean: txnId={}", event.getTransactionId());
        return FraudDecision.clean(steps, elapsed(start));
    }

    // ---------- helpers ----------

    private static long elapsed(long start) {
        return System.currentTimeMillis() - start;
    }

    private static TraceStep step(String stage, String title, String status,
                                  String summary, Map<String, Object> details) {
        return TraceStep.builder()
                .stage(stage).title(title).status(status).summary(summary).details(details)
                .build();
    }

    private static TraceStep skipped(String stage, String title, String why) {
        return step(stage, title, "SKIPPED", why, Map.of());
    }

    /** details("Label", value, "Label 2", value2): keeps insertion order for display. */
    private static Map<String, Object> details(Object... keyValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            map.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
        }
        return map;
    }

    private static String humanDuration(double hours) {
        long seconds = Math.round(hours * 3600);
        if (seconds < 60) return seconds + "s";
        long minutes = seconds / 60;
        if (minutes < 60) return minutes + "m " + (seconds % 60) + "s";
        return (minutes / 60) + "h " + (minutes % 60) + "m";
    }
}
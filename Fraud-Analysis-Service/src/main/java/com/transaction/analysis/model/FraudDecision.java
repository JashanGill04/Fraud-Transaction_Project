package com.transaction.analysis.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudDecision {

    private boolean fraudulent;
    private String reason;
    private Double confidenceScore;
    private List<TraceStep> steps;
    private long durationMs;

    public static FraudDecision clean(List<TraceStep> steps, long durationMs) {
        return FraudDecision.builder()
                .fraudulent(false)
                .steps(steps)
                .durationMs(durationMs)
                .build();
    }

    public static FraudDecision flagged(String reason, Double confidenceScore,
                                        List<TraceStep> steps, long durationMs) {
        return FraudDecision.builder()
                .fraudulent(true)
                .reason(reason)
                .confidenceScore(confidenceScore)
                .steps(steps)
                .durationMs(durationMs)
                .build();
    }
}
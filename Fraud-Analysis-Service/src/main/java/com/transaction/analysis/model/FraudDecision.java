package com.transaction.analysis.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudDecision {

    private boolean fraudulent;
    private String reason;
    private Double confidenceScore;

    public static FraudDecision clean() {
        return FraudDecision.builder()
                .fraudulent(false)
                .build();
    }

    public static FraudDecision flagged(String reason, Double confidenceScore) {
        return FraudDecision.builder()
                .fraudulent(true)
                .reason(reason)
                .confidenceScore(confidenceScore)
                .build();
    }
}
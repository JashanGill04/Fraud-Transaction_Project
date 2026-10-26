package com.transaction.analysis.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Everything that happened to one transaction, stored so the frontend can replay it. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DecisionTrace {
    private UUID transactionId;
    private String maskedCardNumber;
    private BigDecimal amount;
    private String currency;
    private String merchantId;
    private Double latitude;
    private Double longitude;
    private Instant transactionTime;
    private Instant decidedAt;
    private String verdict;   // APPROVED | FLAGGED
    private String reason;    // null when approved
    private long durationMs;
    private List<TraceStep> steps;
}
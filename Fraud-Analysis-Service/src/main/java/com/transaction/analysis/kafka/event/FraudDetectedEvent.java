package com.transaction.analysis.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudDetectedEvent implements Serializable {

    private UUID transactionId;
    private String maskedCardNumber;
    private BigDecimal amount;
    private String merchantId;
    private String reason;        // e.g. "VELOCITY_EXCEEDED", "IMPOSSIBLE_TRAVEL", "AI_ANOMALY"
    private Double confidenceScore; // optional — useful if AI gives a probability
    private Instant detectedAt;
}
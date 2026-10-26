package com.transaction.gateway.kafka.event;

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
public class TransactionDecidedEvent implements Serializable {
    private UUID transactionId;
    private String status;          // APPROVED | FLAGGED
    private String reason;          // null when approved
    private String maskedCardNumber;
    private BigDecimal amount;
    private String currency;
    private String merchantId;
    private Double latitude;
    private Double longitude;
    private Instant decidedAt;
}
package com.transaction.analysis.kafka.event;

import lombok.*;
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
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
public class TransactionCreatedEvent implements Serializable {

    private UUID transactionId;
    private String maskedCardNumber;
    private String cardNumberHash;
    private BigDecimal amount;
    private String merchantId;
    private String currency;
    private Double latitude;
    private Double longitude;
    private Instant timestamp;
}

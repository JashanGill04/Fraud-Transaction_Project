package com.transaction.gateway.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
     private UUID id;

    @Column(nullable = false,length=25)
    private String maskedCardNumber;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String merchantId;

    @Column(nullable = false)
    private String currency;

    private Double latitude;

    private Double longitude;

    @Column(nullable = false)
    private Instant timestamp;
    private String riskReason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;

    public enum TransactionStatus {
        PENDING,
        APPROVED,
        FLAGGED,
        REJECTED
    }


}

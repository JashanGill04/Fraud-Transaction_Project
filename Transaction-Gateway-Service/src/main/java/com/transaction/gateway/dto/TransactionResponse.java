package com.transaction.gateway.dto;
import com.transaction.gateway.entity.Transaction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {

    private UUID transactionId;
    private BigDecimal amount;
    private String merchantId;
    private String status;
    private Instant timestamp;

    public static TransactionResponse fromEntity(Transaction txn) {
        return TransactionResponse.builder()
                .transactionId(txn.getId())
                .amount(txn.getAmount())
                .merchantId(txn.getMerchantId())
                .status(txn.getStatus().name())
                .timestamp(txn.getTimestamp())
                .build();
    }
}

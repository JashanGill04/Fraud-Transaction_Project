package com.transaction.gateway.service;

import com.transaction.gateway.dto.TransactionRequest;
import com.transaction.gateway.dto.TransactionResponse;
import com.transaction.gateway.entity.Transaction;
import com.transaction.gateway.kafka.TransactionProducer;
import com.transaction.gateway.kafka.event.TransactionCreatedEvent;
import com.transaction.gateway.kafka.event.TransactionDecidedEvent;
import com.transaction.gateway.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionProducer transactionProducer;

    @Transactional
    public TransactionResponse processTransaction(TransactionRequest request) {

        UUID transactionId = UUID.randomUUID();
        String maskedCardNumber = maskCardNumber(request.getCardNumber());
        String cardNumberHash = hashCardNumber(request.getCardNumber());

        // 1. Build and persist the entity
        Transaction transaction = Transaction.builder()
                .id(transactionId)
                .maskedCardNumber(maskedCardNumber)
                .amount(request.getAmount())
                .merchantId(request.getMerchantId())
                .currency(request.getCurrency())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .timestamp(Instant.now())
                .status(Transaction.TransactionStatus.PENDING)
                .build();

        transactionRepository.save(transaction);
        log.info("Persisted transaction with id={}", transactionId);

        // 2. Build the Kafka event (never includes the raw card number)
        TransactionCreatedEvent event = TransactionCreatedEvent.builder()
                .transactionId(transactionId)
                .maskedCardNumber(maskedCardNumber)
                .cardNumberHash(cardNumberHash)
                .amount(transaction.getAmount())
                .merchantId(transaction.getMerchantId())
                .currency(transaction.getCurrency())
                .latitude(transaction.getLatitude())
                .longitude(transaction.getLongitude())
                .timestamp(transaction.getTimestamp())
                .build();

        // 3. Publish to Kafka
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                transactionProducer.publish(event);
            }
        });

        // 4. Return response to caller
        return TransactionResponse.fromEntity(transaction);
    }

    public List<TransactionResponse> getTransactions() {
        return transactionRepository.findAll().stream()
                .map(TransactionResponse::fromEntity).toList();
    }

    private String maskCardNumber(String cardNumber) {
        // 4111111111111234 -> 4111-XXXX-XXXX-1234
        String first4 = cardNumber.substring(0, 4);
        String last4 = cardNumber.substring(cardNumber.length() - 4);
        return first4 + "-XXXX-XXXX-" + last4;
    }

    private String hashCardNumber(String cardNumber) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(cardNumber.getBytes());
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is always available on any standard JVM, so this branch
            // is effectively unreachable — but the checked exception must be handled
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    @Transactional
    public void applyDecision(TransactionDecidedEvent event) {
        transactionRepository.findById(event.getTransactionId()).ifPresentOrElse(txn -> {
            txn.setStatus(Transaction.TransactionStatus.valueOf(event.getStatus()));
            txn.setRiskReason(event.getReason());
            transactionRepository.save(txn);
        }, () -> log.warn("Decision received for unknown txnId={}", event.getTransactionId()));
    }

}
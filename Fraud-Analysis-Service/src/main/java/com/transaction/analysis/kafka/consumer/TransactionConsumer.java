package com.transaction.analysis.kafka.consumer;

import com.transaction.analysis.kafka.Producer.FraudEventProducer;
import com.transaction.analysis.kafka.event.FraudDetectedEvent;
import com.transaction.analysis.kafka.event.TransactionCreatedEvent;
import com.transaction.analysis.model.FraudDecision;
import com.transaction.analysis.service.FraudAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionConsumer {

    private final FraudAnalysisService fraudAnalysisService;
    private final FraudEventProducer fraudEventProducer;

    @KafkaListener(
            topics = "transaction.created",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(TransactionCreatedEvent event) {
        log.info("Received transaction.created event: txnId={}", event.getTransactionId());

        FraudDecision decision = fraudAnalysisService.analyze(event);

        if (decision.isFraudulent()) {
            log.warn("FRAUD DETECTED: txnId={}, reason={}", event.getTransactionId(), decision.getReason());

            FraudDetectedEvent fraudEvent = FraudDetectedEvent.builder()
                    .transactionId(event.getTransactionId())
                    .maskedCardNumber(event.getMaskedCardNumber())
                    .amount(event.getAmount())
                    .merchantId(event.getMerchantId())
                    .reason(decision.getReason())
                    .confidenceScore(decision.getConfidenceScore())
                    .detectedAt(Instant.now())
                    .build();

            fraudEventProducer.publish(fraudEvent);
        }
    }
}
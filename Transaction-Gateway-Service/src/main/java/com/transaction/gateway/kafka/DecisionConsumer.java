package com.transaction.gateway.kafka;

import com.transaction.gateway.kafka.event.TransactionDecidedEvent;
import com.transaction.gateway.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DecisionConsumer {

    private final TransactionService transactionService;

    @KafkaListener(
            topics = "transaction.decided",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "decidedListenerFactory"
    )
    public void onDecision(TransactionDecidedEvent event) {
        log.info("Received decision txnId={} status={}", event.getTransactionId(), event.getStatus());
        transactionService.applyDecision(event);
    }
}
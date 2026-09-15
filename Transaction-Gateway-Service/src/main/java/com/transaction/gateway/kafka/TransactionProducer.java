package com.transaction.gateway.kafka;

import com.transaction.gateway.kafka.event.TransactionCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionProducer {

    private static final String TOPIC = "transaction.created";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publish(TransactionCreatedEvent event) {
        kafkaTemplate.send(TOPIC, event.getTransactionId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish transaction.created event for txnId={}",
                                event.getTransactionId(), ex);
                    } else {
                        log.info("Published transaction.created event for txnId={} to partition={}",
                                event.getTransactionId(),
                                result.getRecordMetadata().partition());
                    }
                });
    }
}
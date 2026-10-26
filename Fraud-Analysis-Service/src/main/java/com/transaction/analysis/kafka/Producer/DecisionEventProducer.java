package com.transaction.analysis.kafka.Producer;

import com.transaction.analysis.kafka.event.TransactionDecidedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DecisionEventProducer {

    private static final String TOPIC = "transaction.decided";
    private final KafkaTemplate<String, Object> kafkaTemplate;
    public void publish(TransactionDecidedEvent event) {
        kafkaTemplate.send(TOPIC, event.getTransactionId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish transaction.decided for txnId={}", event.getTransactionId(), ex);
                    } else {
                        log.info("Published transaction.decided txnId={} status={}", event.getTransactionId(), event.getStatus());
                    }
                });
    }
}
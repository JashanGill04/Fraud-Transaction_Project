package com.transaction.analysis.kafka.Producer;

import com.transaction.analysis.kafka.event.FraudDetectedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class FraudEventProducer {

    private static final String TOPIC = "fraud.detected";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publish(FraudDetectedEvent event) {
        kafkaTemplate.send(TOPIC, event.getTransactionId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish fraud.detected event for txnId={}",
                                event.getTransactionId(), ex);
                    } else {
                        log.info("Published fraud.detected event for txnId={} to partition={}",
                                event.getTransactionId(),
                                result.getRecordMetadata().partition());
                    }
                });
    }
}
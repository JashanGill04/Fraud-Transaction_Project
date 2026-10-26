package com.transaction.analysis.ai.service;

import com.transaction.analysis.kafka.event.TransactionCreatedEvent;
import org.springframework.stereotype.Component;

import java.time.ZoneId;

@Component
public class TransactionDescriber {

    public String describe(TransactionCreatedEvent e) {
        double amount = e.getAmount().doubleValue();
        String size = amount < 500 ? "small"
                : amount < 5000 ? "moderate"
                : amount < 20000 ? "high" : "very high";

        int hour = e.getTimestamp().atZone(ZoneId.of("Asia/Kolkata")).getHour();
        String time = hour < 5 ? "late night"
                : hour < 12 ? "morning"
                : hour < 18 ? "afternoon" : "evening";

        return "%s amount purchase (%s %s) at merchant %s, %s"
                .formatted(size, e.getAmount(), e.getCurrency(), e.getMerchantId(), time);
    }
}
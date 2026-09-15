package com.transaction.analysis.ai.service;
import com.transaction.analysis.kafka.event.TransactionCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnomalyDetectionService {

    private final ChatClient.Builder chatClientBuilder;

    public boolean isAnomalous(TransactionCreatedEvent event) {
        ChatClient chatClient = chatClientBuilder.build();

        String prompt = buildPrompt(event);

        String response = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        log.debug("AI anomaly check response for txnId={}: {}", event.getTransactionId(), response);

        return response != null && response.trim().equalsIgnoreCase("FRAUD");
    }

    private String buildPrompt(TransactionCreatedEvent event) {
        return """
                You are a fraud detection assistant. Analyze this credit card transaction
                and respond with exactly one word: "FRAUD" or "LEGIT".

                Transaction details:
                - Amount: %s %s
                - Merchant ID: %s
                - Location: lat=%s, lon=%s
                - Timestamp: %s

                Consider factors like unusually high amount, odd timing, or merchant risk.
                Respond with only one word: FRAUD or LEGIT.
                """.formatted(
                event.getAmount(), event.getCurrency(),
                event.getMerchantId(),
                event.getLatitude(), event.getLongitude(),
                event.getTimestamp()
        );
    }
}
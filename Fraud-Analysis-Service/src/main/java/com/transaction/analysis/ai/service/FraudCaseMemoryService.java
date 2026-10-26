package com.transaction.analysis.ai.service;

import com.transaction.analysis.kafka.event.TransactionCreatedEvent;
import com.transaction.analysis.model.FraudDecision;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class FraudCaseMemoryService {

    private final VectorStore vectorStore;
    private final TransactionDescriber describer;

    public void rememberFlagged(TransactionCreatedEvent event, FraudDecision decision) {
        // Only learn from deterministic rule hits, never from the AI's own verdicts
        if ("AI_ANOMALY".equals(decision.getReason())) {
            return;
        }

        String why = switch (decision.getReason()) {
            case "VELOCITY_EXCEEDED" -> "more than 3 purchases within 60 seconds; the amount itself may have been normal";
            case "IMPOSSIBLE_TRAVEL" -> "location changed faster than a plane could travel; the amount itself may have been normal";
            default -> decision.getReason();
        };

        String text = "Case: %s. Flagged as FRAUD because: %s (not human-confirmed)."
                .formatted(describer.describe(event), why);

        Document doc = new Document(
                "case-" + event.getTransactionId(),
                text,
                Map.of("type", "CASE", "outcome", "FRAUD", "source", "AUTO"));

        try {
            vectorStore.add(List.of(doc));
            log.info("Saved case to knowledge base for txnId={}", event.getTransactionId());
        } catch (Exception ex) {
            // learning is a bonus; it must never break fraud detection itself
            log.warn("Could not save case for txnId={}", event.getTransactionId(), ex);
        }
    }
}
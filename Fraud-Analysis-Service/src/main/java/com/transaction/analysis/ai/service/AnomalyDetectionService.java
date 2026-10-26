package com.transaction.analysis.ai.service;

import com.transaction.analysis.kafka.event.TransactionCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnomalyDetectionService {

    private final ChatClient.Builder chatClientBuilder;
    private final VectorStore vectorStore;
    private final TransactionDescriber describer;

    /**
     * What the AI step did: the search text, the knowledge it was shown (labelled),
     * the model's raw answer, and the verdict.
     */
    public record Result(String query, List<String> retrievedContext, String response, boolean flagged) {}

    public Result analyze(TransactionCreatedEvent event) {
        // 1. RETRIEVE: search the vector store with a text description of the transaction
        String query = describer.describe(event);
        List<Document> raw = vectorStore.similaritySearch(
                SearchRequest.builder().query(query).topK(8).build());
        List<Document> found = raw == null ? List.of() : raw;

        // Balance the evidence: up to 2 fraud cases plus up to 2 other entries (rules, legit cases)
        List<Document> used = new ArrayList<>(found.stream().filter(this::isFraud).limit(2).toList());
        used.addAll(found.stream().filter(d -> !isFraud(d)).limit(2).toList());

        String context = used.isEmpty()
                ? "- (no similar past cases found)"
                : used.stream().map(Document::getText).collect(Collectors.joining("\n- ", "- ", ""));
        log.debug("Retrieved context for txnId={}:\n{}", event.getTransactionId(), context);

        // 2. AUGMENT + GENERATE: put the context in the prompt and ask the model
        String response = chatClientBuilder.build().prompt()
                .user(buildPrompt(event, context))
                .call()
                .content();
        String answer = response == null ? "" : response.trim();
        log.debug("AI anomaly check response for txnId={}: {}", event.getTransactionId(), answer);

        // startsWith tolerates replies like "FRAUD." or "FRAUD - reason"
        boolean flagged = answer.toUpperCase().startsWith("FRAUD");

        return new Result(query, used.stream().map(this::label).toList(), answer, flagged);
    }

    private boolean isFraud(Document d) {
        return "FRAUD".equals(String.valueOf(d.getMetadata().get("outcome")));
    }

    /** "[CASE - FRAUD] text" so the UI can show what kind of knowledge each entry is. */
    private String label(Document d) {
        Object type = d.getMetadata().get("type");
        Object outcome = d.getMetadata().get("outcome");
        String tag = (type == null ? "DOC" : type.toString())
                + (outcome == null ? "" : " - " + outcome);
        return "[" + tag + "] " + d.getText();
    }

    private String buildPrompt(TransactionCreatedEvent event, String context) {
        return """
                You are a fraud detection assistant. Analyze this credit card transaction
                and respond with exactly one word: "FRAUD" or "LEGIT".

                Relevant knowledge (rules and past cases):
                %s

                Notes:
                - Past cases may have been flagged for reasons unrelated to amount (such as purchase frequency).
                - Do not treat similarity to a past case as proof of fraud.
                - If nothing clearly suggests fraud, answer LEGIT.

                Transaction details:
                - Amount: %s %s
                - Merchant ID: %s
                - Location: lat=%s, lon=%s
                - Timestamp: %s

                Respond with only one word: FRAUD or LEGIT.
                """.formatted(
                context,
                event.getAmount(), event.getCurrency(),
                event.getMerchantId(),
                event.getLatitude(), event.getLongitude(),
                event.getTimestamp()
        );
    }
}
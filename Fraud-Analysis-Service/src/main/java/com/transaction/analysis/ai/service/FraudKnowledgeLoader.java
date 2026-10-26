package com.transaction.analysis.ai.service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;


@Component
@RequiredArgsConstructor
@Slf4j
public class FraudKnowledgeLoader implements ApplicationRunner {

    private final VectorStore vectorStore;
    @Override
    public void run(ApplicationArguments args) throws Exception {
        List<Document> docs = List.of(
                new Document("rule-1",
                        "Card testing: many small purchases in quick succession, often at different merchants, "
                                + "followed by one large purchase. Strong fraud signal.",
                        Map.of("type", "RULE")),
                new Document("rule-2",
                        "A very high amount far above normal spending, especially late at night, is suspicious.",
                        Map.of("type", "RULE")),
                new Document("rule-3",
                        "Small regular purchases at the same merchant, such as groceries or subscriptions, are normally legitimate.",
                        Map.of("type", "RULE")),
                new Document("case-seed-1",
                        "Case: very high amount purchase (48000 INR) at merchant MERCH-731, late night. Flagged as FRAUD (confirmed).",
                        Map.of("type", "CASE", "outcome", "FRAUD", "source", "SEED")),
                new Document("case-seed-2",
                        "Case: small amount purchase (250 INR) at merchant MERCH-102, afternoon. Confirmed LEGIT.",
                        Map.of("type", "CASE", "outcome", "LEGIT", "source", "SEED"))
        );
        vectorStore.add(docs);
        log.info("Loaded {} documents into the fraud knowledge base", docs.size());
    }
}

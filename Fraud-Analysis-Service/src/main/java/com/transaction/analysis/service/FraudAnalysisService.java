package com.transaction.analysis.service;

import com.transaction.analysis.ai.service.AnomalyDetectionService;
import com.transaction.analysis.kafka.event.TransactionCreatedEvent;
import com.transaction.analysis.model.FraudDecision;
import com.transaction.analysis.redis.service.GeolocationCheckService;
import com.transaction.analysis.redis.service.VelocityCheckService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class FraudAnalysisService {

    private final VelocityCheckService velocityCheckService;
    private final GeolocationCheckService geolocationCheckService;
    private final AnomalyDetectionService anomalyDetectionService;

    public FraudDecision analyze(TransactionCreatedEvent event) {

        // 1. Fast rule checks first (cheap, no external AI call needed)
        if (velocityCheckService.isVelocityExceeded(event.getCardNumberHash())) {
            log.info("Transaction flagged: VELOCITY_EXCEEDED, txnId={}", event.getTransactionId());
            return FraudDecision.flagged("VELOCITY_EXCEEDED", null);
        }

        if (geolocationCheckService.isImpossibleTravel(
                event.getCardNumberHash(), event.getLatitude(), event.getLongitude(), event.getTimestamp())) {
            log.info("Transaction flagged: IMPOSSIBLE_TRAVEL, txnId={}", event.getTransactionId());
            return FraudDecision.flagged("IMPOSSIBLE_TRAVEL", null);
        }

        // 2. Only fall through to the AI check if rules didn't already catch it
        //    (AI calls are slower/costlier, so we don't want them on every transaction if avoidable —
        //    though for now it still runs on every clean transaction; a smarter version might
        //    only invoke AI for transactions above a certain amount, or sample a percentage)
        boolean aiFlagged = anomalyDetectionService.isAnomalous(event);
        if (aiFlagged) {
            log.info("Transaction flagged: AI_ANOMALY, txnId={}", event.getTransactionId());
            return FraudDecision.flagged("AI_ANOMALY", null);
        }

        log.debug("Transaction clean: txnId={}", event.getTransactionId());
        return FraudDecision.clean();
    }
}
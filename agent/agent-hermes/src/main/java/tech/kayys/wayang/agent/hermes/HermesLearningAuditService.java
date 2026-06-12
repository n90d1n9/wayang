package tech.kayys.wayang.agent.hermes;

import java.util.Map;
import java.util.Optional;

/**
 * Operational read facade for Hermes learned-skill promotion receipts.
 */
public final class HermesLearningAuditService {

    private final HermesLearningPromotionReceiptLedger ledger;

    public HermesLearningAuditService(HermesLearningPromotionReceiptLedger ledger) {
        this.ledger = ledger == null ? HermesLearningPromotionReceiptLedger.noop() : ledger;
    }

    public HermesLearningPromotionReceiptPage query(HermesLearningPromotionReceiptQuery query) {
        return ledger.query(query);
    }

    public HermesLearningAuditRetentionStatus retentionStatus() {
        return HermesLearningAuditRetentionStatus.fromLedger(ledger);
    }

    public Optional<HermesRuntimeEvent> publishRetentionEvent(HermesRuntimeEventSink sink) {
        return new HermesLearningAuditRetentionEventPublisher(sink).publish(retentionStatus());
    }

    public Optional<HermesRuntimeEvent> publishRetentionEventIfChanged(
            HermesLearningAuditRetentionEventMonitor monitor) {
        return observeRetention(monitor).event();
    }

    public HermesLearningAuditRetentionObservation observeRetention(
            HermesLearningAuditRetentionEventMonitor monitor) {
        return monitor == null
                ? HermesLearningAuditRetentionObservation.unavailable("monitor-unavailable")
                : monitor.observe(retentionStatus());
    }

    public Map<String, Object> ledgerMetadata() {
        try {
            return ledger.toMetadata();
        } catch (RuntimeException error) {
            return Map.of(
                    "ledgerType", "unknown",
                    "metadataError", error.getClass().getSimpleName());
        }
    }

    public HermesLearningAuditView inspect(HermesLearningPromotionReceiptQuery query) {
        HermesLearningPromotionReceiptQuery resolved =
                query == null ? HermesLearningPromotionReceiptQuery.recent() : query;
        return HermesLearningAuditView.from(resolved, query(resolved), retentionStatus());
    }

    public HermesLearningAuditView inspectLatest(int limit) {
        return inspect(HermesLearningPromotionReceiptQuery.recent(limit));
    }

    public HermesLearningPromotionReceiptPage latest(int limit) {
        return query(HermesLearningPromotionReceiptQuery.recent(limit));
    }

    public HermesLearningPromotionReceiptPage skill(String skillId, int limit) {
        return query(HermesLearningPromotionReceiptQuery.forSkill(skillId, limit));
    }

    public HermesLearningPromotionReceiptPage status(String status, int limit) {
        return query(HermesLearningPromotionReceiptQuery.forStatus(status, limit));
    }

    public HermesLearningPromotionReceiptPage outcome(String outcome, int limit) {
        return query(HermesLearningPromotionReceiptQuery.forOutcome(outcome, limit));
    }

    public HermesLearningPromotionReceiptPage persisted(int limit) {
        return query(HermesLearningPromotionReceiptQuery.persisted(limit));
    }

    public HermesLearningAuditSummary summarize() {
        return summarize(HermesLearningPromotionReceiptQuery.MAX_LIMIT);
    }

    public HermesLearningAuditSummary summarize(int limit) {
        return HermesLearningAuditSummary.from(latest(limit));
    }
}

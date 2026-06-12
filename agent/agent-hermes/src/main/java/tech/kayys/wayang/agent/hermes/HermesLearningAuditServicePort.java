package tech.kayys.wayang.agent.hermes;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Learning audit port backed by the operational audit service.
 */
public final class HermesLearningAuditServicePort implements HermesLearningAuditPort {

    private final HermesLearningAuditService service;

    public HermesLearningAuditServicePort(HermesLearningAuditService service) {
        this.service = service == null
                ? new HermesLearningAuditService(HermesLearningPromotionReceiptLedger.noop())
                : service;
    }

    @Override
    public HermesPortDispatchResult inspect(HermesLearningAuditDirective directive) {
        HermesLearningAuditDirective resolved = directive == null
                ? HermesLearningAuditDirective.latest(HermesLearningPromotionReceiptQuery.DEFAULT_LIMIT)
                : directive;
        HermesLearningAuditView view = service.inspect(resolved.query());
        HermesLearningAuditRetentionStatus retentionStatus = view.retentionStatus();
        Map<String, Object> metadata = new LinkedHashMap<>(resolved.toMetadata());
        metadata.put("learningAuditView", view.toMetadata());
        metadata.put("learningAuditSummary", view.summary().toMetadata());
        metadata.put("learningAuditRetentionStatus", retentionStatus.toMetadata());
        metadata.put("matchedReceipts", view.page().matchedReceipts());
        metadata.put("totalMatchedReceipts", view.page().totalMatchedReceipts());
        metadata.put("returnedReceipts", view.page().returnedReceipts());
        metadata.put("truncated", view.page().truncated());
        metadata.put("previousCursor", view.page().previousCursor());
        metadata.put("nextCursor", view.page().nextCursor());
        metadata.put("firstCursor", view.page().firstCursor());
        metadata.put("lastCursor", view.page().lastCursor());
        metadata.put("hasPreviousPage", view.page().hasPreviousPage());
        metadata.put("hasNextPage", view.page().hasNextPage());
        metadata.put("cursorResolved", view.page().cursorResolved());
        metadata.put("persistedReceipts", view.summary().persistedReceipts());
        metadata.put("skippedReceipts", view.summary().skippedReceipts());
        metadata.put("rejectedReceipts", view.summary().rejectedReceipts());
        metadata.put("latestSkillId", view.summary().latestSkillId());
        metadata.put("latestOutcome", view.summary().latestOutcome());
        metadata.put("retentionState", retentionStatus.status());
        metadata.put("retentionSeverity", retentionStatus.severity());
        metadata.put("retentionPriority", retentionStatus.priority());
        metadata.put("retentionUtilizationPercent", retentionStatus.utilizationPercent());
        metadata.put("retentionAtCapacity", retentionStatus.atCapacity());
        metadata.put("retentionRemainingEntries", retentionStatus.remainingEntries());
        metadata.put("retentionRequiresAttention", retentionStatus.requiresAttention());
        metadata.put("retentionAttention", retentionStatus.attention());
        metadata.put("retentionRecommendedActions", retentionStatus.recommendedActions());
        return new HermesPortDispatchResult(
                HermesRuntimePortCatalog.LEARNING_AUDIT,
                resolved.operation(),
                resolved.target(),
                true,
                true,
                true,
                "inspected",
                "learning audit inspected",
                metadata);
    }

    @Override
    public HermesRuntimePortDescriptor descriptor() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("ledger", service.ledgerMetadata());
        metadata.put("retentionStatus", service.retentionStatus().toMetadata());
        return new HermesRuntimePortDescriptor(
                HermesRuntimePortCatalog.LEARNING_AUDIT,
                getClass().getName(),
                "service",
                true,
                false,
                true,
                "ready",
                "learning audit service configured",
                metadata);
    }
}

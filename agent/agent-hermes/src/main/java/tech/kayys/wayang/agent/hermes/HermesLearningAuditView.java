package tech.kayys.wayang.agent.hermes;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Adapter-facing learning audit view that bundles query, page, and summary.
 */
public record HermesLearningAuditView(
        HermesLearningPromotionReceiptQuery query,
        HermesLearningPromotionReceiptPage page,
        HermesLearningAuditSummary summary,
        HermesLearningAuditRetentionStatus retentionStatus) {

    public HermesLearningAuditView(
            HermesLearningPromotionReceiptQuery query,
            HermesLearningPromotionReceiptPage page,
            HermesLearningAuditSummary summary) {
        this(query, page, summary, null);
    }

    public HermesLearningAuditView {
        query = query == null ? HermesLearningPromotionReceiptQuery.recent() : query;
        page = page == null ? HermesLearningPromotionReceiptPage.empty(query) : page;
        summary = summary == null ? HermesLearningAuditSummary.from(page) : summary;
        retentionStatus = retentionStatus == null
                ? HermesLearningAuditRetentionStatus.fromMetadata(Map.of())
                : retentionStatus;
    }

    public static HermesLearningAuditView from(
            HermesLearningPromotionReceiptQuery query,
            HermesLearningPromotionReceiptPage page) {
        return from(query, page, null);
    }

    public static HermesLearningAuditView from(
            HermesLearningPromotionReceiptQuery query,
            HermesLearningPromotionReceiptPage page,
            HermesLearningAuditRetentionStatus retentionStatus) {
        HermesLearningPromotionReceiptPage resolvedPage = page == null
                ? HermesLearningPromotionReceiptPage.empty(query)
                : page;
        HermesLearningPromotionReceiptQuery resolvedQuery = query == null
                ? resolvedPage.query()
                : query;
        return new HermesLearningAuditView(
                resolvedQuery,
                resolvedPage,
                HermesLearningAuditSummary.from(resolvedPage),
                retentionStatus);
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("query", query.toMetadata());
        values.put("page", page.toMetadata());
        values.put("summary", summary.toMetadata());
        values.put("retentionStatus", retentionStatus.toMetadata());
        values.put("matchedReceipts", page.matchedReceipts());
        values.put("totalMatchedReceipts", page.totalMatchedReceipts());
        values.put("returnedReceipts", page.returnedReceipts());
        values.put("truncated", page.truncated());
        values.put("previousCursor", page.previousCursor());
        values.put("nextCursor", page.nextCursor());
        values.put("firstCursor", page.firstCursor());
        values.put("lastCursor", page.lastCursor());
        values.put("hasPreviousPage", page.hasPreviousPage());
        values.put("hasNextPage", page.hasNextPage());
        values.put("cursorResolved", page.cursorResolved());
        values.put("persistedReceipts", summary.persistedReceipts());
        values.put("skippedReceipts", summary.skippedReceipts());
        values.put("rejectedReceipts", summary.rejectedReceipts());
        values.put("latestSkillId", summary.latestSkillId());
        values.put("latestOutcome", summary.latestOutcome());
        values.put("retentionState", retentionStatus.status());
        values.put("retentionSeverity", retentionStatus.severity());
        values.put("retentionPriority", retentionStatus.priority());
        values.put("retentionUtilizationPercent", retentionStatus.utilizationPercent());
        values.put("retentionAtCapacity", retentionStatus.atCapacity());
        values.put("retentionRemainingEntries", retentionStatus.remainingEntries());
        values.put("retentionRequiresAttention", retentionStatus.requiresAttention());
        values.put("retentionAttention", retentionStatus.attention());
        values.put("retentionRecommendedActions", retentionStatus.recommendedActions());
        return Map.copyOf(values);
    }
}

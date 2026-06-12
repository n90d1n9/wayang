package tech.kayys.wayang.agent.api;

import tech.kayys.wayang.agent.hermes.HermesRuntimeEvent;

import java.util.List;
import java.util.Map;

/**
 * Domain-specific projection of a learning-audit retention journal event.
 */
public record HermesLearningAuditRetentionEventResponse(
        String eventId,
        String type,
        String requestId,
        String tenantId,
        String sessionId,
        String userId,
        String outcome,
        String occurredAt,
        String source,
        Map<String, Object> metadata,
        Map<String, Object> learningAuditRetentionStatus,
        String ledgerType,
        boolean bounded,
        int recordCount,
        int maxEntries,
        int remainingEntries,
        int overflowEntries,
        int utilizationPercent,
        boolean nearCapacity,
        boolean atCapacity,
        Map<String, Object> retentionPolicy,
        String retentionStatus,
        String retentionSeverity,
        int retentionPriority,
        boolean retentionRequiresAttention,
        List<String> retentionAttention,
        List<HermesOperationalAttention> retentionAttentionItems,
        List<String> retentionRecommendedActions,
        List<HermesOperationalAction> retentionRecommendedActionItems) {

    public HermesLearningAuditRetentionEventResponse {
        eventId = HermesResponseMetadata.text(eventId, "");
        type = HermesResponseMetadata.text(type, HermesRuntimeEvent.TYPE_LEARNING_AUDIT_RETENTION_ATTENTION);
        requestId = HermesResponseMetadata.text(requestId, "");
        tenantId = HermesResponseMetadata.text(tenantId, "");
        sessionId = HermesResponseMetadata.text(sessionId, "");
        userId = HermesResponseMetadata.text(userId, "");
        outcome = HermesResponseMetadata.text(outcome, "");
        occurredAt = HermesResponseMetadata.text(occurredAt, "");
        source = HermesResponseMetadata.text(source, "learning-audit-retention");
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        learningAuditRetentionStatus = learningAuditRetentionStatus == null
                ? Map.of()
                : Map.copyOf(learningAuditRetentionStatus);
        ledgerType = HermesResponseMetadata.text(ledgerType, "unknown");
        retentionPolicy = retentionPolicy == null ? Map.of() : Map.copyOf(retentionPolicy);
        retentionStatus = HermesResponseMetadata.text(retentionStatus, "");
        retentionSeverity = HermesResponseMetadata.text(retentionSeverity, "");
        retentionPriority = Math.max(retentionPriority, 0);
        retentionAttention = retentionAttention == null ? List.of() : List.copyOf(retentionAttention);
        retentionAttentionItems = retentionAttentionItems == null
                ? HermesOperationalAttention.fromMessages(
                        source,
                        retentionSeverity,
                        retentionPriority,
                        retentionAttention)
                : List.copyOf(retentionAttentionItems);
        retentionRecommendedActions = retentionRecommendedActions == null
                ? List.of()
                : List.copyOf(retentionRecommendedActions);
        retentionRecommendedActionItems = retentionRecommendedActionItems == null
                ? HermesOperationalAction.retentionActions(
                        retentionSeverity,
                        retentionPriority,
                        retentionRecommendedActions)
                : List.copyOf(retentionRecommendedActionItems);
    }

    static boolean isRetentionEvent(HermesJournalEventResponse event) {
        if (event == null) {
            return false;
        }
        return HermesRuntimeEvent.TYPE_LEARNING_AUDIT_RETENTION_ATTENTION.equals(event.type())
                || "learning-audit-retention".equals(HermesResponseMetadata.text(event.metadata().get("source"), ""))
                || !HermesResponseMetadata.learningAuditRetentionStatus(event.metadata()).isEmpty();
    }

    static HermesLearningAuditRetentionEventResponse from(HermesJournalEventResponse event) {
        HermesJournalEventResponse resolved = event == null
                ? HermesJournalEventResponse.from(Map.of())
                : event;
        Map<String, Object> metadata = resolved.metadata();
        HermesLearningAuditRetentionProjection retention =
                HermesLearningAuditRetentionProjection.from(
                        HermesResponseMetadata.learningAuditRetentionStatus(metadata));
        return new HermesLearningAuditRetentionEventResponse(
                resolved.eventId(),
                resolved.type(),
                resolved.requestId(),
                resolved.tenantId(),
                resolved.sessionId(),
                resolved.userId(),
                resolved.outcome(),
                resolved.occurredAt(),
                HermesResponseMetadata.text(metadata.get("source"), "learning-audit-retention"),
                metadata,
                retention.metadata(),
                retention.ledgerType(),
                retention.bounded(),
                retention.recordCount(),
                retention.maxEntries(),
                retention.remainingEntries(),
                retention.overflowEntries(),
                retention.utilizationPercent(),
                retention.nearCapacity(),
                retention.atCapacity(),
                HermesResponseMetadata.objectMap(retention.metadata().get("retentionPolicy")),
                retention.status(),
                retention.severity(),
                retention.priority(),
                retention.requiresAttention(),
                retention.attention(),
                null,
                retention.recommendedActions(),
                null);
    }
}

package tech.kayys.wayang.agent.api;

import tech.kayys.wayang.agent.hermes.HermesRuntimePortCatalog;
import tech.kayys.wayang.agent.hermes.HermesRuntimePortDescriptor;

import java.util.List;
import java.util.Map;

/**
 * Compact REST payload for learning-audit retention pressure and capacity.
 */
public record HermesLearningAuditRetentionResponse(
        String port,
        boolean configured,
        boolean noop,
        boolean ready,
        String status,
        String reason,
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
        String retentionStatus,
        String retentionSeverity,
        int retentionPriority,
        boolean retentionRequiresAttention,
        List<String> retentionAttention,
        List<HermesOperationalAttention> retentionAttentionItems,
        List<String> retentionRecommendedActions,
        List<HermesOperationalAction> retentionRecommendedActionItems) {

    public HermesLearningAuditRetentionResponse {
        port = HermesResponseMetadata.text(port, HermesRuntimePortCatalog.LEARNING_AUDIT);
        status = HermesResponseMetadata.text(status, ready ? "ready" : "unavailable");
        reason = HermesResponseMetadata.text(reason, "");
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        learningAuditRetentionStatus = learningAuditRetentionStatus == null
                ? Map.of()
                : Map.copyOf(learningAuditRetentionStatus);
        ledgerType = HermesResponseMetadata.text(ledgerType, "unknown");
        retentionStatus = HermesResponseMetadata.text(retentionStatus, "");
        retentionSeverity = HermesResponseMetadata.text(retentionSeverity, "");
        retentionAttention = retentionAttention == null ? List.of() : List.copyOf(retentionAttention);
        retentionAttentionItems = retentionAttentionItems == null
                ? HermesOperationalAttention.fromMessages(
                        "learning-audit-retention",
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

    public HermesLearningAuditRetentionResponse(
            String port,
            boolean configured,
            boolean noop,
            boolean ready,
            String status,
            String reason,
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
            String retentionStatus,
            String retentionSeverity,
            int retentionPriority,
            boolean retentionRequiresAttention,
            List<String> retentionAttention,
            List<String> retentionRecommendedActions) {
        this(
                port,
                configured,
                noop,
                ready,
                status,
                reason,
                metadata,
                learningAuditRetentionStatus,
                ledgerType,
                bounded,
                recordCount,
                maxEntries,
                remainingEntries,
                overflowEntries,
                utilizationPercent,
                nearCapacity,
                atCapacity,
                retentionStatus,
                retentionSeverity,
                retentionPriority,
                retentionRequiresAttention,
                retentionAttention,
                null,
                retentionRecommendedActions,
                null);
    }

    public static HermesLearningAuditRetentionResponse from(HermesRuntimePortDescriptor descriptor) {
        HermesRuntimePortDescriptor resolved = descriptor == null
                ? HermesRuntimePortDescriptor.noop(HermesRuntimePortCatalog.LEARNING_AUDIT)
                : descriptor;
        Map<String, Object> metadata = resolved.metadata();
        HermesLearningAuditRetentionProjection retention =
                HermesLearningAuditRetentionProjection.from(
                        HermesResponseMetadata.learningAuditRetentionStatus(metadata));
        return new HermesLearningAuditRetentionResponse(
                resolved.port(),
                resolved.configured(),
                resolved.noop(),
                resolved.ready(),
                resolved.status(),
                resolved.reason(),
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

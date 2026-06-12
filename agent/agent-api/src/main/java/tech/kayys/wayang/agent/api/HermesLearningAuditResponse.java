package tech.kayys.wayang.agent.api;

import java.util.Map;

/**
 * Typed REST payload for Hermes learned-skill audit inspections.
 */
public record HermesLearningAuditResponse(
        String port,
        String operation,
        String target,
        boolean active,
        boolean dispatched,
        boolean successful,
        String status,
        String reason,
        Map<String, Object> metadata,
        int matchedReceipts,
        int totalMatchedReceipts,
        int returnedReceipts,
        boolean truncated,
        String previousCursor,
        String nextCursor,
        String firstCursor,
        String lastCursor,
        boolean hasPreviousPage,
        boolean hasNextPage,
        boolean cursorResolved,
        long persistedReceipts,
        long skippedReceipts,
        long rejectedReceipts,
        String latestSkillId,
        String latestOutcome,
        Map<String, Object> query,
        Map<String, Object> learningAuditView,
        Map<String, Object> learningAuditSummary,
        Map<String, Object> learningAuditRetentionStatus) {

    public HermesLearningAuditResponse {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        previousCursor = HermesResponseMetadata.text(previousCursor, "");
        nextCursor = HermesResponseMetadata.text(nextCursor, "");
        firstCursor = HermesResponseMetadata.text(firstCursor, "");
        lastCursor = HermesResponseMetadata.text(lastCursor, "");
        latestSkillId = HermesResponseMetadata.text(latestSkillId, "");
        latestOutcome = HermesResponseMetadata.text(latestOutcome, "");
        query = query == null ? Map.of() : Map.copyOf(query);
        learningAuditView = learningAuditView == null ? Map.of() : Map.copyOf(learningAuditView);
        learningAuditSummary = learningAuditSummary == null ? Map.of() : Map.copyOf(learningAuditSummary);
        learningAuditRetentionStatus = learningAuditRetentionStatus == null
                ? Map.of()
                : Map.copyOf(learningAuditRetentionStatus);
    }

    public static HermesLearningAuditResponse from(HermesPortResponse response) {
        HermesPortResponse resolved = response == null ? HermesPortResponse.from(null) : response;
        Map<String, Object> metadata = resolved.metadata();
        return new HermesLearningAuditResponse(
                resolved.port(),
                resolved.operation(),
                resolved.target(),
                resolved.active(),
                resolved.dispatched(),
                resolved.successful(),
                resolved.status(),
                resolved.reason(),
                metadata,
                HermesResponseMetadata.integer(metadata.get("matchedReceipts")),
                HermesResponseMetadata.integer(metadata.get("totalMatchedReceipts")),
                HermesResponseMetadata.integer(metadata.get("returnedReceipts")),
                HermesResponseMetadata.bool(metadata.get("truncated")),
                HermesResponseMetadata.text(metadata.get("previousCursor"), ""),
                HermesResponseMetadata.text(metadata.get("nextCursor"), ""),
                HermesResponseMetadata.text(metadata.get("firstCursor"), ""),
                HermesResponseMetadata.text(metadata.get("lastCursor"), ""),
                HermesResponseMetadata.bool(metadata.get("hasPreviousPage")),
                HermesResponseMetadata.bool(metadata.get("hasNextPage")),
                HermesResponseMetadata.bool(metadata.get("cursorResolved")),
                HermesResponseMetadata.longValue(metadata.get("persistedReceipts")),
                HermesResponseMetadata.longValue(metadata.get("skippedReceipts")),
                HermesResponseMetadata.longValue(metadata.get("rejectedReceipts")),
                HermesResponseMetadata.text(metadata.get("latestSkillId"), ""),
                HermesResponseMetadata.text(metadata.get("latestOutcome"), ""),
                HermesResponseMetadata.objectMap(metadata.get("query")),
                HermesResponseMetadata.objectMap(metadata.get("learningAuditView")),
                HermesResponseMetadata.objectMap(metadata.get("learningAuditSummary")),
                HermesResponseMetadata.learningAuditRetentionStatus(metadata));
    }
}

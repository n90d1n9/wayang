package tech.kayys.wayang.agenticcommerce.wayang;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Compact operator summary for persistence transfer audit trails.
 */
public record AgenticCommerceWayangPersistenceTransferAuditSummary(
        String trailType,
        String trailStatus,
        String outcomeStatus,
        boolean successful,
        boolean requiresAttention,
        boolean operatorActionRecommended,
        boolean dryRun,
        boolean forced,
        boolean mutatedTarget,
        boolean blocked,
        int eventCount,
        int successfulEventCount,
        int blockedEventCount,
        int issueCount,
        int warningCount,
        int findingCount,
        int plannedDocumentCount,
        int copyableDocumentCount,
        int copiedDocumentCount,
        int skippedDocumentCount,
        int blockedDocumentCount,
        List<String> eventTypes,
        List<String> eventStatuses,
        List<String> recommendationActions,
        List<String> attentionReasons,
        Map<String, Object> attributes) {

    public static final String OUTCOME_READY = "ready";
    public static final String OUTCOME_COMPLETE = "complete";
    public static final String OUTCOME_PREVIEW = "preview";
    public static final String OUTCOME_ATTENTION = "attention";
    public static final String OUTCOME_FORCED = "forced";
    public static final String OUTCOME_BLOCKED = "blocked";
    public static final String OUTCOME_FAILED = "failed";
    public static final String OUTCOME_NOOP = "noop";

    public AgenticCommerceWayangPersistenceTransferAuditSummary {
        trailType = AgenticCommerceWayangMaps.required(trailType, "trailType");
        trailStatus = AgenticCommerceWayangMaps.required(trailStatus, "trailStatus");
        outcomeStatus = normalizeOutcome(outcomeStatus);
        eventTypes = AgenticCommerceWayangMaps.stringList(eventTypes);
        eventStatuses = AgenticCommerceWayangMaps.stringList(eventStatuses);
        recommendationActions = AgenticCommerceWayangMaps.stringList(recommendationActions);
        attentionReasons = AgenticCommerceWayangMaps.stringList(attentionReasons);
        attributes = AgenticCommerceWayangMaps.copy(attributes);
    }

    public static AgenticCommerceWayangPersistenceTransferAuditSummary from(
            AgenticCommerceWayangPersistenceTransferAuditTrail trail) {
        AgenticCommerceWayangPersistenceTransferAuditTrail resolved =
                Objects.requireNonNull(trail, "trail");
        List<String> recommendationActions = resolved.recommendationActions();
        List<String> attentionReasons = resolved.attentionReasons();
        boolean requiresAttention = requiresAttention(resolved, attentionReasons);
        return new AgenticCommerceWayangPersistenceTransferAuditSummary(
                resolved.trailType(),
                resolved.trailStatus(),
                outcomeStatus(resolved),
                resolved.successful(),
                requiresAttention,
                operatorActionRecommended(resolved, recommendationActions),
                resolved.dryRun(),
                resolved.forced(),
                resolved.mutatedTarget(),
                resolved.blocked(),
                resolved.eventCount(),
                resolved.successfulEventCount(),
                resolved.blockedEventCount(),
                resolved.issueCount(),
                resolved.warningCount(),
                resolved.findingCount(),
                resolved.plannedDocumentCount(),
                resolved.copyableDocumentCount(),
                resolved.copiedDocumentCount(),
                resolved.skippedDocumentCount(),
                resolved.blockedDocumentCount(),
                resolved.eventTypes(),
                resolved.eventStatuses(),
                recommendationActions,
                attentionReasons,
                attributes(resolved, requiresAttention));
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("trailType", trailType);
        values.put("trailStatus", trailStatus);
        values.put("outcomeStatus", outcomeStatus);
        values.put("successful", successful);
        values.put("requiresAttention", requiresAttention);
        values.put("operatorActionRecommended", operatorActionRecommended);
        values.put("dryRun", dryRun);
        values.put("forced", forced);
        values.put("mutatedTarget", mutatedTarget);
        values.put("blocked", blocked);
        values.put("eventCount", eventCount);
        values.put("successfulEventCount", successfulEventCount);
        values.put("blockedEventCount", blockedEventCount);
        values.put("issueCount", issueCount);
        values.put("warningCount", warningCount);
        values.put("findingCount", findingCount);
        values.put("plannedDocumentCount", plannedDocumentCount);
        values.put("copyableDocumentCount", copyableDocumentCount);
        values.put("copiedDocumentCount", copiedDocumentCount);
        values.put("skippedDocumentCount", skippedDocumentCount);
        values.put("blockedDocumentCount", blockedDocumentCount);
        values.put("eventTypes", eventTypes);
        values.put("eventStatuses", eventStatuses);
        values.put("recommendationActions", recommendationActions);
        values.put("attentionReasons", attentionReasons);
        values.put("decision", decision().toMap());
        values.put("attributes", attributes);
        return Map.copyOf(values);
    }

    public AgenticCommerceWayangPersistenceTransferAuditDecision decision() {
        return AgenticCommerceWayangPersistenceTransferAuditDecision.from(this);
    }

    private static String outcomeStatus(AgenticCommerceWayangPersistenceTransferAuditTrail trail) {
        String trailStatus = trail.trailStatus();
        if (AgenticCommerceWayangPersistenceTransferApplyReport.STATUS_NOOP.equals(trailStatus)
                || AgenticCommerceWayangPersistenceTransferSummary.STATUS_NOOP.equals(trailStatus)) {
            return OUTCOME_NOOP;
        }
        if (!trail.successful() && trail.blocked()) {
            return OUTCOME_BLOCKED;
        }
        if (!trail.successful()) {
            return OUTCOME_FAILED;
        }
        if (trail.forced()) {
            return OUTCOME_FORCED;
        }
        if (trail.dryRun()) {
            return OUTCOME_PREVIEW;
        }
        if (AgenticCommerceWayangPersistenceTransferPreflightReport.STATUS_READY.equals(trailStatus)) {
            return OUTCOME_READY;
        }
        if (trail.blocked()) {
            return OUTCOME_ATTENTION;
        }
        return OUTCOME_COMPLETE;
    }

    private static boolean requiresAttention(
            AgenticCommerceWayangPersistenceTransferAuditTrail trail,
            List<String> attentionReasons) {
        if (!trail.successful() || trail.blocked() || trail.forced() || trail.dryRun()) {
            return true;
        }
        if (AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_PREFLIGHT.equals(trail.trailType())) {
            return trail.warningCount() > 0 || !attentionReasons.isEmpty();
        }
        return trail.issueCount() > 0;
    }

    private static boolean operatorActionRecommended(
            AgenticCommerceWayangPersistenceTransferAuditTrail trail,
            List<String> recommendationActions) {
        if (recommendationActions.isEmpty()) {
            return false;
        }
        return !trail.successful()
                || trail.blocked()
                || trail.forced()
                || AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_PREFLIGHT.equals(
                        trail.trailType());
    }

    private static String normalizeOutcome(String outcomeStatus) {
        String normalized = AgenticCommerceWayangMaps.text(outcomeStatus);
        return normalized.isBlank() ? OUTCOME_FAILED : normalized;
    }

    private static Map<String, Object> attributes(
            AgenticCommerceWayangPersistenceTransferAuditTrail trail,
            boolean requiresAttention) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("latestEventType", trail.latestEvent().eventType());
        values.put("latestEventStatus", trail.latestEvent().eventStatus());
        values.put("hasPreflightEvent", trail.hasEventType(
                AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_PREFLIGHT));
        values.put("hasCopyEvent", trail.hasEventType(
                AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_COPY));
        values.put("hasApplyEvent", trail.hasEventType(
                AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_APPLY));
        values.put("requiresAttention", requiresAttention);
        return Map.copyOf(values);
    }
}

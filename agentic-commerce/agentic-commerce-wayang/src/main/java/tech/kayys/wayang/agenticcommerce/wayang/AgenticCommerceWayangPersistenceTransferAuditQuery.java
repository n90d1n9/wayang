package tech.kayys.wayang.agenticcommerce.wayang;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Query filters for persistence transfer audit trails.
 */
public record AgenticCommerceWayangPersistenceTransferAuditQuery(
        String trailType,
        String outcomeStatus,
        String nextAction,
        String decisionStatus,
        Boolean requiresAttention,
        int limit) {

    public static final int DEFAULT_LIMIT = 100;
    public static final int MAX_LIMIT = 1_000;

    public AgenticCommerceWayangPersistenceTransferAuditQuery {
        trailType = AgenticCommerceWayangMaps.text(trailType);
        outcomeStatus = AgenticCommerceWayangMaps.text(outcomeStatus);
        nextAction = AgenticCommerceWayangMaps.text(nextAction);
        decisionStatus = AgenticCommerceWayangMaps.text(decisionStatus);
        limit = normalizeLimit(limit);
    }

    public static AgenticCommerceWayangPersistenceTransferAuditQuery latest(int limit) {
        return new AgenticCommerceWayangPersistenceTransferAuditQuery("", "", "", "", null, limit);
    }

    public static AgenticCommerceWayangPersistenceTransferAuditQuery byType(String trailType, int limit) {
        return new AgenticCommerceWayangPersistenceTransferAuditQuery(trailType, "", "", "", null, limit);
    }

    public static AgenticCommerceWayangPersistenceTransferAuditQuery byOutcome(String outcomeStatus, int limit) {
        return new AgenticCommerceWayangPersistenceTransferAuditQuery("", outcomeStatus, "", "", null, limit);
    }

    public static AgenticCommerceWayangPersistenceTransferAuditQuery byNextAction(String nextAction, int limit) {
        return new AgenticCommerceWayangPersistenceTransferAuditQuery("", "", nextAction, "", null, limit);
    }

    public static AgenticCommerceWayangPersistenceTransferAuditQuery requiringAttention(int limit) {
        return new AgenticCommerceWayangPersistenceTransferAuditQuery("", "", "", "", true, limit);
    }

    public boolean matches(AgenticCommerceWayangPersistenceTransferAuditTrail trail) {
        if (trail == null) {
            return false;
        }
        AgenticCommerceWayangPersistenceTransferAuditSummary summary = trail.summary();
        AgenticCommerceWayangPersistenceTransferAuditDecision decision = summary.decision();
        return matchesText(trailType, trail.trailType())
                && matchesText(outcomeStatus, summary.outcomeStatus())
                && matchesText(nextAction, decision.nextAction())
                && matchesText(decisionStatus, decision.decisionStatus())
                && (requiresAttention == null || requiresAttention == summary.requiresAttention());
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("trailType", trailType);
        values.put("outcomeStatus", outcomeStatus);
        values.put("nextAction", nextAction);
        values.put("decisionStatus", decisionStatus);
        if (requiresAttention != null) {
            values.put("requiresAttention", requiresAttention);
        }
        values.put("limit", limit);
        return Map.copyOf(values);
    }

    private static boolean matchesText(String expected, String actual) {
        return expected == null || expected.isBlank() || expected.equals(AgenticCommerceWayangMaps.text(actual));
    }

    private static int normalizeLimit(int limit) {
        if (limit < 1) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }
}

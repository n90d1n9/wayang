package tech.kayys.wayang.agenticcommerce.wayang;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Stage-level readiness check for persistence transfer preflight reports.
 */
public record AgenticCommerceWayangPersistenceTransferPreflightCheck(
        String checkId,
        String stage,
        String checkStatus,
        boolean passed,
        boolean ready,
        boolean blocking,
        int issueCount,
        int warningCount,
        int findingCount,
        List<String> attentionReasons,
        Map<String, Object> attributes) {

    public static final String STATUS_PASSED = "passed";
    public static final String STATUS_WARNING = "warning";
    public static final String STATUS_INCOMPLETE = "incomplete";
    public static final String STATUS_BLOCKED = "blocked";
    public static final String STATUS_FAILED = "failed";
    public static final String STATUS_NOOP = "noop";

    public static final String CHECK_SOURCE_HEALTH = "source_health";
    public static final String CHECK_TARGET_HEALTH = "target_health";
    public static final String CHECK_TRANSFER_PLAN = "transfer_plan";

    public AgenticCommerceWayangPersistenceTransferPreflightCheck {
        checkId = AgenticCommerceWayangMaps.required(checkId, "checkId");
        stage = AgenticCommerceWayangMaps.required(stage, "stage");
        checkStatus = normalizeStatus(checkStatus);
        attentionReasons = AgenticCommerceWayangMaps.stringList(attentionReasons);
        attributes = AgenticCommerceWayangMaps.copy(attributes);
    }

    public static AgenticCommerceWayangPersistenceTransferPreflightCheck sourceHealth(
            AgenticCommerceWayangPersistenceHealthReport health) {
        AgenticCommerceWayangPersistenceHealthReport resolved =
                java.util.Objects.requireNonNull(health, "health");
        boolean passed = resolved.ready();
        boolean ready = resolved.ready() && resolved.complete();
        List<String> attentionReasons = new ArrayList<>();
        addIf(attentionReasons, !resolved.ready(), "source_health_issues");
        addIf(attentionReasons, resolved.ready() && !resolved.complete(), "source_documents_missing");
        addIf(attentionReasons, resolved.warningCount() > 0, "source_health_warnings");
        return new AgenticCommerceWayangPersistenceTransferPreflightCheck(
                CHECK_SOURCE_HEALTH,
                "source",
                healthStatus(resolved, true),
                passed,
                ready,
                !ready,
                resolved.issueCount(),
                resolved.warningCount(),
                resolved.findingCount(),
                attentionReasons,
                healthAttributes(resolved));
    }

    public static AgenticCommerceWayangPersistenceTransferPreflightCheck targetHealth(
            AgenticCommerceWayangPersistenceHealthReport health) {
        AgenticCommerceWayangPersistenceHealthReport resolved =
                java.util.Objects.requireNonNull(health, "health");
        boolean ready = resolved.ready();
        List<String> attentionReasons = new ArrayList<>();
        addIf(attentionReasons, !resolved.ready(), "target_health_issues");
        addIf(attentionReasons, resolved.warningCount() > 0, "target_health_warnings");
        return new AgenticCommerceWayangPersistenceTransferPreflightCheck(
                CHECK_TARGET_HEALTH,
                "target",
                healthStatus(resolved, false),
                resolved.ready(),
                ready,
                !ready,
                resolved.issueCount(),
                resolved.warningCount(),
                resolved.findingCount(),
                attentionReasons,
                healthAttributes(resolved));
    }

    public static AgenticCommerceWayangPersistenceTransferPreflightCheck transferPlan(
            AgenticCommerceWayangPersistenceTransferPlan plan) {
        AgenticCommerceWayangPersistenceTransferPlan resolved =
                java.util.Objects.requireNonNull(plan, "plan");
        boolean blocked = resolved.blockedDocumentCount() > 0;
        boolean ready = resolved.passed() && resolved.wouldMutateTarget() && !blocked;
        List<String> attentionReasons = new ArrayList<>();
        addIf(attentionReasons, !resolved.passed(), "transfer_plan_issues");
        addIf(attentionReasons, blocked, "transfer_blocked_documents");
        addIf(attentionReasons, resolved.passed() && !resolved.wouldMutateTarget(), "target_would_not_change");
        return new AgenticCommerceWayangPersistenceTransferPreflightCheck(
                CHECK_TRANSFER_PLAN,
                "plan",
                planStatus(resolved, blocked),
                resolved.passed(),
                ready,
                !resolved.passed() || blocked,
                resolved.issueCount(),
                resolved.warningFindingCount(),
                resolved.findingCount(),
                attentionReasons,
                planAttributes(resolved));
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("checkId", checkId);
        values.put("stage", stage);
        values.put("checkStatus", checkStatus);
        values.put("passed", passed);
        values.put("ready", ready);
        values.put("blocking", blocking);
        values.put("issueCount", issueCount);
        values.put("warningCount", warningCount);
        values.put("findingCount", findingCount);
        values.put("attentionReasons", attentionReasons);
        values.put("attributes", attributes);
        return Map.copyOf(values);
    }

    private static String healthStatus(
            AgenticCommerceWayangPersistenceHealthReport health,
            boolean completeRequired) {
        if (!health.ready()) {
            return STATUS_FAILED;
        }
        if (completeRequired && !health.complete()) {
            return STATUS_INCOMPLETE;
        }
        if (health.warningCount() > 0) {
            return STATUS_WARNING;
        }
        return STATUS_PASSED;
    }

    private static String planStatus(
            AgenticCommerceWayangPersistenceTransferPlan plan,
            boolean blocked) {
        if (!plan.passed()) {
            return STATUS_FAILED;
        }
        if (blocked) {
            return STATUS_BLOCKED;
        }
        if (!plan.wouldMutateTarget()) {
            return STATUS_NOOP;
        }
        return STATUS_PASSED;
    }

    private static Map<String, Object> healthAttributes(
            AgenticCommerceWayangPersistenceHealthReport health) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("healthStatus", health.healthStatus());
        values.put("complete", health.complete());
        values.put("availableDocumentCount", health.availableDocumentCount());
        values.put("requiredDocumentCount", health.requiredDocumentCount());
        values.put("missingDocumentIds", health.documentIndex().missingIds());
        values.put("failedDocumentIds", health.documentIndex().failedIds());
        values.put("persistenceTarget", health.persistenceTarget());
        return Map.copyOf(values);
    }

    private static Map<String, Object> planAttributes(
            AgenticCommerceWayangPersistenceTransferPlan plan) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("transferStatus", plan.summary().transferStatus());
        values.put("plannedDocumentCount", plan.plannedDocumentCount());
        values.put("copyableDocumentCount", plan.copyableDocumentCount());
        values.put("skippedDocumentCount", plan.skippedDocumentCount());
        values.put("blockedDocumentCount", plan.blockedDocumentCount());
        values.put("wouldMutateTarget", plan.wouldMutateTarget());
        values.put("blockedDocumentIds", plan.blockedDocuments());
        values.put("skippedDocumentIds", plan.skippedDocuments());
        return Map.copyOf(values);
    }

    private static void addIf(List<String> values, boolean condition, String value) {
        if (condition) {
            values.add(value);
        }
    }

    private static String normalizeStatus(String value) {
        String normalized = AgenticCommerceWayangMaps.text(value);
        if (STATUS_PASSED.equals(normalized)
                || STATUS_WARNING.equals(normalized)
                || STATUS_INCOMPLETE.equals(normalized)
                || STATUS_BLOCKED.equals(normalized)
                || STATUS_FAILED.equals(normalized)
                || STATUS_NOOP.equals(normalized)) {
            return normalized;
        }
        return STATUS_FAILED;
    }
}

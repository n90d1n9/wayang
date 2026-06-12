package tech.kayys.wayang.agenticcommerce.wayang;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Compact audit event for persistence transfer preflight, apply, and copy operations.
 */
public record AgenticCommerceWayangPersistenceTransferAuditEvent(
        String eventType,
        String eventStatus,
        boolean successful,
        boolean dryRun,
        boolean forced,
        boolean mutatedTarget,
        boolean blocked,
        int issueCount,
        int warningCount,
        int findingCount,
        int plannedDocumentCount,
        int copyableDocumentCount,
        int copiedDocumentCount,
        int skippedDocumentCount,
        int blockedDocumentCount,
        List<String> recommendationActions,
        List<String> attentionReasons,
        Map<String, Object> attributes) {

    public static final String TYPE_TRANSFER_PREFLIGHT = "persistence_transfer_preflight";
    public static final String TYPE_TRANSFER_APPLY = "persistence_transfer_apply";
    public static final String TYPE_TRANSFER_COPY = "persistence_transfer_copy";

    public AgenticCommerceWayangPersistenceTransferAuditEvent {
        eventType = AgenticCommerceWayangMaps.required(eventType, "eventType");
        eventStatus = AgenticCommerceWayangMaps.required(eventStatus, "eventStatus");
        recommendationActions = AgenticCommerceWayangMaps.stringList(recommendationActions);
        attentionReasons = AgenticCommerceWayangMaps.stringList(attentionReasons);
        attributes = AgenticCommerceWayangMaps.copy(attributes);
    }

    public static AgenticCommerceWayangPersistenceTransferAuditEvent from(
            AgenticCommerceWayangPersistenceTransferPreflightReport preflight) {
        AgenticCommerceWayangPersistenceTransferPreflightReport resolved =
                java.util.Objects.requireNonNull(preflight, "preflight");
        AgenticCommerceWayangPersistenceTransferPlan plan = resolved.plan();
        return new AgenticCommerceWayangPersistenceTransferAuditEvent(
                TYPE_TRANSFER_PREFLIGHT,
                resolved.preflightStatus(),
                resolved.readyToApply(),
                resolved.options().dryRun(),
                false,
                false,
                resolved.blocked() || !resolved.readyToApply(),
                resolved.issueCount(),
                resolved.warningCount(),
                resolved.findingCount(),
                plan.plannedDocumentCount(),
                plan.copyableDocumentCount(),
                0,
                plan.skippedDocumentCount(),
                plan.blockedDocumentCount(),
                resolved.recommendationActions(),
                resolved.attentionReasons(),
                preflightAttributes(resolved));
    }

    public static AgenticCommerceWayangPersistenceTransferAuditEvent fromMap(Map<?, ?> values) {
        Map<String, Object> resolved = AgenticCommerceWayangMaps.copy(values);
        return new AgenticCommerceWayangPersistenceTransferAuditEvent(
                AgenticCommerceWayangMaps.firstText(resolved, "eventType", "type"),
                AgenticCommerceWayangMaps.firstText(resolved, "eventStatus", "status"),
                AgenticCommerceWayangMaps.firstBoolean(resolved, "successful", "success", "passed")
                        .orElse(false),
                AgenticCommerceWayangMaps.firstBoolean(resolved, "dryRun", "preview").orElse(false),
                AgenticCommerceWayangMaps.firstBoolean(resolved, "forced", "force").orElse(false),
                AgenticCommerceWayangMaps.firstBoolean(
                        resolved,
                        "mutatedTarget",
                        "targetMutated",
                        "mutated").orElse(false),
                AgenticCommerceWayangMaps.firstBoolean(resolved, "blocked", "blockedByPreflight")
                        .orElse(false),
                AgenticCommerceWayangMaps.firstInt(resolved, 0, "issueCount", "issues"),
                AgenticCommerceWayangMaps.firstInt(resolved, 0, "warningCount", "warnings"),
                AgenticCommerceWayangMaps.firstInt(resolved, 0, "findingCount", "findings"),
                AgenticCommerceWayangMaps.firstInt(resolved, 0, "plannedDocumentCount", "plannedDocuments"),
                AgenticCommerceWayangMaps.firstInt(resolved, 0, "copyableDocumentCount", "copyableDocuments"),
                AgenticCommerceWayangMaps.firstInt(resolved, 0, "copiedDocumentCount", "copiedDocuments"),
                AgenticCommerceWayangMaps.firstInt(resolved, 0, "skippedDocumentCount", "skippedDocuments"),
                AgenticCommerceWayangMaps.firstInt(resolved, 0, "blockedDocumentCount", "blockedDocuments"),
                AgenticCommerceWayangMaps.stringList(AgenticCommerceWayangMaps.first(
                        resolved,
                        "recommendationActions",
                        "recommendations",
                        "actions")),
                AgenticCommerceWayangMaps.stringList(AgenticCommerceWayangMaps.first(
                        resolved,
                        "attentionReasons",
                        "attention",
                        "reasons")),
                AgenticCommerceWayangMaps.firstMap(resolved, "attributes", "metadata", "details"));
    }

    public static AgenticCommerceWayangPersistenceTransferAuditEvent from(
            AgenticCommerceWayangPersistenceTransferReport report) {
        AgenticCommerceWayangPersistenceTransferReport resolved =
                java.util.Objects.requireNonNull(report, "report");
        AgenticCommerceWayangPersistenceTransferSummary summary = resolved.summary();
        return new AgenticCommerceWayangPersistenceTransferAuditEvent(
                TYPE_TRANSFER_COPY,
                summary.transferStatus(),
                resolved.passed(),
                resolved.options().dryRun(),
                false,
                summary.mutatedTarget(),
                summary.blocked(),
                resolved.issueCount(),
                0,
                resolved.findingCount(),
                resolved.plannedDocumentCount(),
                summary.copyableDocumentCount(),
                resolved.copiedDocumentCount(),
                resolved.skippedDocumentCount(),
                resolved.blockedDocumentCount(),
                List.of(),
                summary.attentionReasons(),
                transferAttributes(resolved));
    }

    public static AgenticCommerceWayangPersistenceTransferAuditEvent from(
            AgenticCommerceWayangPersistenceTransferApplyReport report) {
        AgenticCommerceWayangPersistenceTransferApplyReport resolved =
                java.util.Objects.requireNonNull(report, "report");
        AgenticCommerceWayangPersistenceTransferPreflightReport preflight = resolved.preflight();
        AgenticCommerceWayangPersistenceTransferReport transferReport = resolved.transferReport();
        AgenticCommerceWayangPersistenceTransferPlan plan = preflight.plan();
        int copiedCount = transferReport == null ? 0 : transferReport.copiedDocumentCount();
        int blockedCount = transferReport == null ? plan.blockedDocumentCount() : transferReport.blockedDocumentCount();
        int skippedCount = transferReport == null ? plan.skippedDocumentCount() : transferReport.skippedDocumentCount();
        return new AgenticCommerceWayangPersistenceTransferAuditEvent(
                TYPE_TRANSFER_APPLY,
                resolved.applyStatus(),
                resolved.passed(),
                preflight.options().dryRun(),
                resolved.forced(),
                resolved.mutatedTarget(),
                resolved.blockedByPreflight() || preflight.blocked(),
                resolved.issueCount(),
                resolved.warningCount(),
                preflight.findingCount() + (transferReport == null ? 0 : transferReport.findingCount()),
                plan.plannedDocumentCount(),
                plan.copyableDocumentCount(),
                copiedCount,
                skippedCount,
                blockedCount,
                preflight.recommendationActions(),
                preflight.attentionReasons(),
                applyAttributes(resolved));
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("eventType", eventType);
        values.put("eventStatus", eventStatus);
        values.put("successful", successful);
        values.put("dryRun", dryRun);
        values.put("forced", forced);
        values.put("mutatedTarget", mutatedTarget);
        values.put("blocked", blocked);
        values.put("issueCount", issueCount);
        values.put("warningCount", warningCount);
        values.put("findingCount", findingCount);
        values.put("plannedDocumentCount", plannedDocumentCount);
        values.put("copyableDocumentCount", copyableDocumentCount);
        values.put("copiedDocumentCount", copiedDocumentCount);
        values.put("skippedDocumentCount", skippedDocumentCount);
        values.put("blockedDocumentCount", blockedDocumentCount);
        values.put("recommendationActions", recommendationActions);
        values.put("attentionReasons", attentionReasons);
        values.put("attributes", attributes);
        return Map.copyOf(values);
    }

    private static Map<String, Object> preflightAttributes(
            AgenticCommerceWayangPersistenceTransferPreflightReport preflight) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("sourceStoreKind", preflight.sourceHealth().storageKind());
        values.put("targetStoreKind", preflight.targetHealth().storageKind());
        values.put("sourceHealthStatus", preflight.sourceHealth().healthStatus());
        values.put("targetHealthStatus", preflight.targetHealth().healthStatus());
        values.put("readyCheckCount", preflight.readyCheckCount());
        values.put("blockingCheckCount", preflight.blockingCheckCount());
        values.put("recommendationCount", preflight.recommendationCount());
        values.put("blockingRecommendationCount", preflight.blockingRecommendationCount());
        values.put("sourcePersistenceTarget", preflight.sourceHealth().persistenceTarget());
        values.put("targetPersistenceTarget", preflight.targetHealth().persistenceTarget());
        return Map.copyOf(values);
    }

    private static Map<String, Object> transferAttributes(
            AgenticCommerceWayangPersistenceTransferReport report) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("sourceStoreKind", report.sourceStoreKind());
        values.put("targetStoreKind", report.targetStoreKind());
        values.put("sourcePersistenceTarget", report.sourcePersistenceTarget());
        values.put("targetPersistenceTarget", report.targetPersistenceTargetAfter());
        values.put("targetChangeReasons", report.persistenceTargetComparisonAfter().changeReasons());
        return Map.copyOf(values);
    }

    private static Map<String, Object> applyAttributes(
            AgenticCommerceWayangPersistenceTransferApplyReport report) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("transferAttempted", report.transferAttempted());
        values.put("transferReportAvailable", report.transferReportAvailable());
        values.put("blockedByPreflight", report.blockedByPreflight());
        values.put("preflightStatus", report.preflight().preflightStatus());
        values.put("preflightReadyToApply", report.preflight().readyToApply());
        values.put("sourcePersistenceTarget", report.preflight().sourceHealth().persistenceTarget());
        values.put("targetPersistenceTarget", report.preflight().targetHealth().persistenceTarget());
        if (report.transferReportAvailable()) {
            values.put("transferStatus", report.transferReport().summary().transferStatus());
        }
        return Map.copyOf(values);
    }
}

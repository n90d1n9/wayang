package tech.kayys.wayang.agenticcommerce.wayang;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Guarded apply report for persistence transfers.
 */
public record AgenticCommerceWayangPersistenceTransferApplyReport(
        AgenticCommerceWayangPersistenceTransferPreflightReport preflight,
        AgenticCommerceWayangPersistenceTransferReport transferReport,
        boolean forced,
        Map<String, Object> attributes) {

    public static final String STATUS_APPLIED = "applied";
    public static final String STATUS_FORCED = "forced";
    public static final String STATUS_BLOCKED = "blocked";
    public static final String STATUS_FAILED = "failed";
    public static final String STATUS_DRY_RUN = "dry_run";
    public static final String STATUS_NOOP = "noop";

    public AgenticCommerceWayangPersistenceTransferApplyReport {
        preflight = Objects.requireNonNull(preflight, "preflight");
        attributes = AgenticCommerceWayangMaps.copy(attributes);
    }

    public static AgenticCommerceWayangPersistenceTransferApplyReport blocked(
            AgenticCommerceWayangPersistenceTransferPreflightReport preflight) {
        return new AgenticCommerceWayangPersistenceTransferApplyReport(
                preflight,
                null,
                false,
                attributes(false));
    }

    public static AgenticCommerceWayangPersistenceTransferApplyReport applied(
            AgenticCommerceWayangPersistenceTransferPreflightReport preflight,
            AgenticCommerceWayangPersistenceTransferReport transferReport,
            boolean forced) {
        return new AgenticCommerceWayangPersistenceTransferApplyReport(
                preflight,
                Objects.requireNonNull(transferReport, "transferReport"),
                forced,
                attributes(forced));
    }

    public boolean transferAttempted() {
        return transferReport != null;
    }

    public boolean transferReportAvailable() {
        return transferAttempted();
    }

    public boolean applied() {
        return transferAttempted() && transferReport.summary().mutatedTarget();
    }

    public boolean passed() {
        return transferAttempted() && transferReport.passed();
    }

    public boolean failed() {
        return transferAttempted() && !transferReport.passed();
    }

    public boolean blockedByPreflight() {
        return !transferAttempted() && !preflight.readyToApply();
    }

    public boolean noop() {
        return !transferAttempted()
                && AgenticCommerceWayangPersistenceTransferPreflightReport.STATUS_NOOP.equals(
                        preflight.preflightStatus());
    }

    public boolean mutatedTarget() {
        return transferAttempted() && transferReport.summary().mutatedTarget();
    }

    public String applyStatus() {
        if (!transferAttempted()) {
            return noop() ? STATUS_NOOP : STATUS_BLOCKED;
        }
        if (!transferReport.passed()) {
            return STATUS_FAILED;
        }
        if (transferReport.options().dryRun()) {
            return STATUS_DRY_RUN;
        }
        if (forced) {
            return STATUS_FORCED;
        }
        return STATUS_APPLIED;
    }

    public int issueCount() {
        return preflight.issueCount() + (transferAttempted() ? transferReport.issueCount() : 0);
    }

    public int warningCount() {
        return preflight.warningCount();
    }

    public AgenticCommerceWayangPersistenceTransferAuditEvent auditEvent() {
        return AgenticCommerceWayangPersistenceTransferAuditEvent.from(this);
    }

    public AgenticCommerceWayangPersistenceTransferAuditTrail auditTrail() {
        return AgenticCommerceWayangPersistenceTransferAuditTrail.from(this);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("applyStatus", applyStatus());
        values.put("passed", passed());
        values.put("applied", applied());
        values.put("forced", forced);
        values.put("failed", failed());
        values.put("noop", noop());
        values.put("mutatedTarget", mutatedTarget());
        values.put("blockedByPreflight", blockedByPreflight());
        values.put("transferAttempted", transferAttempted());
        values.put("transferReportAvailable", transferReportAvailable());
        values.put("issueCount", issueCount());
        values.put("warningCount", warningCount());
        values.put("preflightStatus", preflight.preflightStatus());
        values.put("preflightReadyToApply", preflight.readyToApply());
        values.put("preflightRecommendationActions", preflight.recommendationActions());
        values.put("auditEvent", auditEvent().toMap());
        values.put("auditTrail", auditTrail().toMap());
        values.put("preflight", preflight.toMap());
        if (transferAttempted()) {
            values.put("transferStatus", transferReport.summary().transferStatus());
            values.put("transferCopiedDocumentCount", transferReport.copiedDocumentCount());
            values.put("transferBlockedDocumentCount", transferReport.blockedDocumentCount());
            values.put("transferReport", transferReport.toMap());
        }
        values.put("attributes", attributes);
        return Map.copyOf(values);
    }

    private static Map<String, Object> attributes(boolean forced) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("applyId", "agentic-commerce-wayang-persistence-transfer-apply");
        values.put("forced", forced);
        return Map.copyOf(values);
    }
}

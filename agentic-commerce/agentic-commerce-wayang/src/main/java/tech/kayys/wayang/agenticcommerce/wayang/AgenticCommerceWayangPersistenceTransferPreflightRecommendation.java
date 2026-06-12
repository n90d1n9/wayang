package tech.kayys.wayang.agenticcommerce.wayang;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Operator action derived from a persistence transfer preflight report.
 */
public record AgenticCommerceWayangPersistenceTransferPreflightRecommendation(
        String action,
        String priority,
        String title,
        String rationale,
        boolean blocking,
        List<String> checkIds,
        Map<String, Object> attributes) {

    public static final String PRIORITY_PRIMARY = "primary";
    public static final String PRIORITY_SECONDARY = "secondary";

    public static final String ACTION_APPLY_TRANSFER = "apply_transfer";
    public static final String ACTION_COMPLETE_SOURCE_DOCUMENTS = "complete_source_documents";
    public static final String ACTION_FIX_SOURCE_HEALTH = "fix_source_health";
    public static final String ACTION_INSPECT_TARGET_HEALTH = "inspect_target_health";
    public static final String ACTION_REVIEW_TRANSFER_PLAN = "review_transfer_plan";
    public static final String ACTION_ENABLE_OVERWRITE_OR_CLEAR_TARGET = "enable_overwrite_or_clear_target";
    public static final String ACTION_REVIEW_SOURCE_WARNINGS = "review_source_warnings";
    public static final String ACTION_REVIEW_TARGET_WARNINGS = "review_target_warnings";
    public static final String ACTION_NOOP_TRANSFER = "noop_transfer";

    public AgenticCommerceWayangPersistenceTransferPreflightRecommendation {
        action = AgenticCommerceWayangMaps.required(action, "action");
        priority = normalizePriority(priority);
        title = AgenticCommerceWayangMaps.text(title);
        rationale = AgenticCommerceWayangMaps.text(rationale);
        checkIds = AgenticCommerceWayangMaps.stringList(checkIds);
        attributes = AgenticCommerceWayangMaps.copy(attributes);
    }

    public static List<AgenticCommerceWayangPersistenceTransferPreflightRecommendation> from(
            AgenticCommerceWayangPersistenceTransferPreflightReport report) {
        if (report == null) {
            return List.of();
        }
        List<AgenticCommerceWayangPersistenceTransferPreflightRecommendation> values = new ArrayList<>();
        if (!report.sourceHealth().ready()) {
            values.add(fixSourceHealth(report));
        } else if (!report.sourceHealth().complete()) {
            values.add(completeSourceDocuments(report));
        }
        if (!report.targetHealth().ready()) {
            values.add(inspectTargetHealth(report));
        }
        if (!report.plan().passed()) {
            values.add(reviewTransferPlan(report));
        } else if (report.blocked()) {
            values.add(resolveBlockedDocuments(report));
        } else if (report.readyToApply()) {
            values.add(applyTransfer(report));
        } else if (report.passed() && !report.wouldMutateTarget() && values.isEmpty()) {
            values.add(noopTransfer(report));
        }
        if (report.sourceHealth().ready()
                && report.sourceHealth().complete()
                && report.sourceHealth().warningCount() > 0) {
            values.add(reviewSourceWarnings(report));
        }
        if (report.targetHealth().ready() && report.targetHealth().warningCount() > 0) {
            values.add(reviewTargetWarnings(report));
        }
        return List.copyOf(values);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("action", action);
        values.put("priority", priority);
        values.put("title", title);
        values.put("rationale", rationale);
        values.put("blocking", blocking);
        values.put("checkIds", checkIds);
        values.put("attributes", attributes);
        return Map.copyOf(values);
    }

    private static AgenticCommerceWayangPersistenceTransferPreflightRecommendation applyTransfer(
            AgenticCommerceWayangPersistenceTransferPreflightReport report) {
        return recommendation(
                ACTION_APPLY_TRANSFER,
                PRIORITY_PRIMARY,
                "Apply transfer",
                "Source and target checks passed, and the plan has copyable documents.",
                false,
                List.of(AgenticCommerceWayangPersistenceTransferPreflightCheck.CHECK_TRANSFER_PLAN),
                Map.of(
                        "copyableDocumentCount",
                        report.plan().copyableDocumentCount(),
                        "copyableDocumentIds",
                        report.plan().summary().copyableDocumentIds()));
    }

    private static AgenticCommerceWayangPersistenceTransferPreflightRecommendation completeSourceDocuments(
            AgenticCommerceWayangPersistenceTransferPreflightReport report) {
        return recommendation(
                ACTION_COMPLETE_SOURCE_DOCUMENTS,
                PRIORITY_PRIMARY,
                "Complete source documents",
                "The source store is readable but does not contain every required transfer document.",
                true,
                List.of(AgenticCommerceWayangPersistenceTransferPreflightCheck.CHECK_SOURCE_HEALTH),
                Map.of("missingDocumentIds", report.sourceHealth().documentIndex().missingIds()));
    }

    private static AgenticCommerceWayangPersistenceTransferPreflightRecommendation fixSourceHealth(
            AgenticCommerceWayangPersistenceTransferPreflightReport report) {
        return recommendation(
                ACTION_FIX_SOURCE_HEALTH,
                PRIORITY_PRIMARY,
                "Fix source persistence health",
                "The source store has blocking health issues and cannot be trusted as a transfer source.",
                true,
                List.of(AgenticCommerceWayangPersistenceTransferPreflightCheck.CHECK_SOURCE_HEALTH),
                Map.of(
                        "issueCount",
                        report.sourceHealth().issueCount(),
                        "failedDocumentIds",
                        report.sourceHealth().documentIndex().failedIds()));
    }

    private static AgenticCommerceWayangPersistenceTransferPreflightRecommendation inspectTargetHealth(
            AgenticCommerceWayangPersistenceTransferPreflightReport report) {
        return recommendation(
                ACTION_INSPECT_TARGET_HEALTH,
                PRIORITY_PRIMARY,
                "Inspect target persistence health",
                "The target store has blocking health issues and may not accept transferred state.",
                true,
                List.of(AgenticCommerceWayangPersistenceTransferPreflightCheck.CHECK_TARGET_HEALTH),
                Map.of(
                        "issueCount",
                        report.targetHealth().issueCount(),
                        "failedDocumentIds",
                        report.targetHealth().documentIndex().failedIds()));
    }

    private static AgenticCommerceWayangPersistenceTransferPreflightRecommendation reviewTransferPlan(
            AgenticCommerceWayangPersistenceTransferPreflightReport report) {
        return recommendation(
                ACTION_REVIEW_TRANSFER_PLAN,
                PRIORITY_PRIMARY,
                "Review transfer plan issues",
                "The transfer plan contains blocking issues that should be resolved before applying.",
                true,
                List.of(AgenticCommerceWayangPersistenceTransferPreflightCheck.CHECK_TRANSFER_PLAN),
                Map.of(
                        "issueCount",
                        report.plan().issueCount(),
                        "findingCount",
                        report.plan().findingCount()));
    }

    private static AgenticCommerceWayangPersistenceTransferPreflightRecommendation resolveBlockedDocuments(
            AgenticCommerceWayangPersistenceTransferPreflightReport report) {
        return recommendation(
                ACTION_ENABLE_OVERWRITE_OR_CLEAR_TARGET,
                PRIORITY_PRIMARY,
                "Resolve blocked target documents",
                "The no-overwrite policy found existing target documents that block part of the transfer.",
                true,
                List.of(AgenticCommerceWayangPersistenceTransferPreflightCheck.CHECK_TRANSFER_PLAN),
                Map.of(
                        "blockedDocumentCount",
                        report.plan().blockedDocumentCount(),
                        "blockedDocumentIds",
                        report.plan().blockedDocuments(),
                        "overwriteExisting",
                        report.options().overwriteExisting()));
    }

    private static AgenticCommerceWayangPersistenceTransferPreflightRecommendation reviewSourceWarnings(
            AgenticCommerceWayangPersistenceTransferPreflightReport report) {
        return recommendation(
                ACTION_REVIEW_SOURCE_WARNINGS,
                PRIORITY_SECONDARY,
                "Review source health warnings",
                "The source store can be used, but it reported non-blocking health warnings.",
                false,
                List.of(AgenticCommerceWayangPersistenceTransferPreflightCheck.CHECK_SOURCE_HEALTH),
                Map.of("warningCount", report.sourceHealth().warningCount()));
    }

    private static AgenticCommerceWayangPersistenceTransferPreflightRecommendation reviewTargetWarnings(
            AgenticCommerceWayangPersistenceTransferPreflightReport report) {
        return recommendation(
                ACTION_REVIEW_TARGET_WARNINGS,
                PRIORITY_SECONDARY,
                "Review target health warnings",
                "The target store can be used, but it reported non-blocking health warnings.",
                false,
                List.of(AgenticCommerceWayangPersistenceTransferPreflightCheck.CHECK_TARGET_HEALTH),
                Map.of("warningCount", report.targetHealth().warningCount()));
    }

    private static AgenticCommerceWayangPersistenceTransferPreflightRecommendation noopTransfer(
            AgenticCommerceWayangPersistenceTransferPreflightReport report) {
        return recommendation(
                ACTION_NOOP_TRANSFER,
                PRIORITY_SECONDARY,
                "No transfer needed",
                "The preflight passed, but the transfer plan has no target mutation to apply.",
                false,
                List.of(AgenticCommerceWayangPersistenceTransferPreflightCheck.CHECK_TRANSFER_PLAN),
                Map.of(
                        "skippedDocumentCount",
                        report.plan().skippedDocumentCount(),
                        "plannedDocumentCount",
                        report.plan().plannedDocumentCount()));
    }

    private static AgenticCommerceWayangPersistenceTransferPreflightRecommendation recommendation(
            String action,
            String priority,
            String title,
            String rationale,
            boolean blocking,
            List<String> checkIds,
            Map<String, Object> attributes) {
        return new AgenticCommerceWayangPersistenceTransferPreflightRecommendation(
                action,
                priority,
                title,
                rationale,
                blocking,
                checkIds,
                attributes);
    }

    private static String normalizePriority(String value) {
        String normalized = AgenticCommerceWayangMaps.text(value);
        if (PRIORITY_PRIMARY.equals(normalized) || PRIORITY_SECONDARY.equals(normalized)) {
            return normalized;
        }
        return PRIORITY_SECONDARY;
    }
}

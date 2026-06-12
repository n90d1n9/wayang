package tech.kayys.wayang.agenticcommerce.wayang;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Compact operator summary for persistence transfer reports and plans.
 */
public record AgenticCommerceWayangPersistenceTransferSummary(
        String transferStatus,
        boolean passed,
        boolean planningOnly,
        boolean dryRun,
        boolean complete,
        boolean partial,
        boolean blocked,
        boolean failed,
        boolean noop,
        boolean mutatedTarget,
        boolean wouldMutateTarget,
        boolean targetChanged,
        int plannedDocumentCount,
        int copyableDocumentCount,
        int copiedDocumentCount,
        int skippedDocumentCount,
        int blockedDocumentCount,
        int failedDocumentCount,
        int issueCount,
        List<String> plannedDocumentIds,
        List<String> copyableDocumentIds,
        List<String> copiedDocumentIds,
        List<String> skippedDocumentIds,
        List<String> blockedDocumentIds,
        List<String> failedDocumentIds,
        List<String> attentionReasons,
        Map<String, Object> attributes) {

    public static final String STATUS_COMPLETE = "complete";
    public static final String STATUS_PREVIEW = "preview";
    public static final String STATUS_PARTIAL = "partial";
    public static final String STATUS_BLOCKED = "blocked";
    public static final String STATUS_SKIPPED = "skipped";
    public static final String STATUS_FAILED = "failed";
    public static final String STATUS_NOOP = "noop";

    public AgenticCommerceWayangPersistenceTransferSummary {
        transferStatus = normalizeStatus(transferStatus);
        plannedDocumentIds = AgenticCommerceWayangMaps.stringList(plannedDocumentIds);
        copyableDocumentIds = AgenticCommerceWayangMaps.stringList(copyableDocumentIds);
        copiedDocumentIds = AgenticCommerceWayangMaps.stringList(copiedDocumentIds);
        skippedDocumentIds = AgenticCommerceWayangMaps.stringList(skippedDocumentIds);
        blockedDocumentIds = AgenticCommerceWayangMaps.stringList(blockedDocumentIds);
        failedDocumentIds = AgenticCommerceWayangMaps.stringList(failedDocumentIds);
        attentionReasons = AgenticCommerceWayangMaps.stringList(attentionReasons);
        attributes = AgenticCommerceWayangMaps.copy(attributes);
    }

    public static AgenticCommerceWayangPersistenceTransferSummary from(
            AgenticCommerceWayangPersistenceTransferReport report) {
        if (report == null) {
            return missingReport(false);
        }
        List<AgenticCommerceWayangPersistenceTransferDocumentStatus> documents = report.documents();
        boolean dryRun = report.options().dryRun();
        boolean planningOnly = false;
        boolean passed = report.passed();
        int plannedCount = report.plannedDocumentCount();
        int copiedCount = report.copiedDocumentCount();
        int copyableCount = copyableDocumentIds(documents).size();
        int skippedCount = report.skippedDocumentCount();
        int blockedCount = report.blockedDocumentCount();
        int failedCount = failedDocumentIds(documents).size();
        boolean complete = complete(
                passed,
                planningOnly,
                dryRun,
                plannedCount,
                copyableCount,
                copiedCount,
                skippedCount,
                blockedCount,
                failedCount);
        boolean partial = partial(
                passed,
                complete,
                plannedCount,
                copyableCount,
                copiedCount,
                skippedCount,
                blockedCount);
        boolean noop = noop(passed, plannedCount, copyableCount, copiedCount);
        boolean failed = !passed || failedCount > 0;
        boolean targetChanged = report.persistenceTargetComparisonAfter().changed();
        boolean mutatedTarget = passed && !dryRun && copiedCount > 0;
        boolean wouldMutateTarget = passed && dryRun && copyableCount > 0;
        return new AgenticCommerceWayangPersistenceTransferSummary(
                status(
                        passed,
                        planningOnly,
                        dryRun,
                        complete,
                        partial,
                        noop,
                        copyableCount,
                        copiedCount,
                        skippedCount,
                        blockedCount,
                        failedCount),
                passed,
                planningOnly,
                dryRun,
                complete,
                partial,
                blockedCount > 0,
                failed,
                noop,
                mutatedTarget,
                wouldMutateTarget,
                targetChanged,
                plannedCount,
                copyableCount,
                copiedCount,
                skippedCount,
                blockedCount,
                failedCount,
                report.issueCount(),
                report.plannedDocuments(),
                copyableDocumentIds(documents),
                report.copiedDocuments(),
                report.skippedDocuments(),
                report.blockedDocuments(),
                failedDocumentIds(documents),
                attentionReasons(planningOnly, dryRun, passed, copyableCount, skippedCount, blockedCount, failedCount),
                reportAttributes(report));
    }

    public static AgenticCommerceWayangPersistenceTransferSummary from(
            AgenticCommerceWayangPersistenceTransferPlan plan) {
        if (plan == null) {
            return missingReport(true);
        }
        List<AgenticCommerceWayangPersistenceTransferDocumentStatus> documents = plan.documents();
        boolean dryRun = plan.options().dryRun();
        boolean planningOnly = true;
        boolean passed = plan.passed();
        int plannedCount = plan.plannedDocumentCount();
        int copiedCount = 0;
        int copyableCount = plan.copyableDocumentCount();
        int skippedCount = plan.skippedDocumentCount();
        int blockedCount = plan.blockedDocumentCount();
        int failedCount = failedDocumentIds(documents).size();
        boolean complete = complete(
                passed,
                planningOnly,
                dryRun,
                plannedCount,
                copyableCount,
                copiedCount,
                skippedCount,
                blockedCount,
                failedCount);
        boolean partial = partial(
                passed,
                complete,
                plannedCount,
                copyableCount,
                copiedCount,
                skippedCount,
                blockedCount);
        boolean noop = noop(passed, plannedCount, copyableCount, copiedCount);
        boolean failed = !passed || failedCount > 0;
        boolean targetChanged = plan.persistenceTargetComparison().changed();
        return new AgenticCommerceWayangPersistenceTransferSummary(
                status(
                        passed,
                        planningOnly,
                        dryRun,
                        complete,
                        partial,
                        noop,
                        copyableCount,
                        copiedCount,
                        skippedCount,
                        blockedCount,
                        failedCount),
                passed,
                planningOnly,
                dryRun,
                complete,
                partial,
                blockedCount > 0,
                failed,
                noop,
                false,
                plan.wouldMutateTarget(),
                targetChanged,
                plannedCount,
                copyableCount,
                copiedCount,
                skippedCount,
                blockedCount,
                failedCount,
                plan.issueCount(),
                plan.plannedDocuments(),
                copyableDocumentIds(documents),
                List.of(),
                plan.skippedDocuments(),
                plan.blockedDocuments(),
                failedDocumentIds(documents),
                attentionReasons(planningOnly, dryRun, passed, copyableCount, skippedCount, blockedCount, failedCount),
                planAttributes(plan));
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("transferStatus", transferStatus);
        values.put("passed", passed);
        values.put("planningOnly", planningOnly);
        values.put("dryRun", dryRun);
        values.put("complete", complete);
        values.put("partial", partial);
        values.put("blocked", blocked);
        values.put("failed", failed);
        values.put("noop", noop);
        values.put("mutatedTarget", mutatedTarget);
        values.put("wouldMutateTarget", wouldMutateTarget);
        values.put("targetChanged", targetChanged);
        values.put("plannedDocumentCount", plannedDocumentCount);
        values.put("copyableDocumentCount", copyableDocumentCount);
        values.put("copiedDocumentCount", copiedDocumentCount);
        values.put("skippedDocumentCount", skippedDocumentCount);
        values.put("blockedDocumentCount", blockedDocumentCount);
        values.put("failedDocumentCount", failedDocumentCount);
        values.put("issueCount", issueCount);
        values.put("plannedDocumentIds", plannedDocumentIds);
        values.put("copyableDocumentIds", copyableDocumentIds);
        values.put("copiedDocumentIds", copiedDocumentIds);
        values.put("skippedDocumentIds", skippedDocumentIds);
        values.put("blockedDocumentIds", blockedDocumentIds);
        values.put("failedDocumentIds", failedDocumentIds);
        values.put("attentionReasons", attentionReasons);
        values.put("attributes", attributes);
        return Map.copyOf(values);
    }

    private static AgenticCommerceWayangPersistenceTransferSummary missingReport(boolean planningOnly) {
        return new AgenticCommerceWayangPersistenceTransferSummary(
                STATUS_FAILED,
                false,
                planningOnly,
                false,
                false,
                false,
                false,
                true,
                false,
                false,
                false,
                false,
                0,
                0,
                0,
                0,
                0,
                0,
                1,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of("transfer_missing"),
                Map.of("reason", "transfer_missing"));
    }

    private static boolean complete(
            boolean passed,
            boolean planningOnly,
            boolean dryRun,
            int plannedCount,
            int copyableCount,
            int copiedCount,
            int skippedCount,
            int blockedCount,
            int failedCount) {
        if (!passed || plannedCount == 0 || skippedCount > 0 || blockedCount > 0 || failedCount > 0) {
            return false;
        }
        if (planningOnly || dryRun) {
            return copyableCount == plannedCount;
        }
        return copiedCount == plannedCount;
    }

    private static boolean partial(
            boolean passed,
            boolean complete,
            int plannedCount,
            int copyableCount,
            int copiedCount,
            int skippedCount,
            int blockedCount) {
        return passed
                && !complete
                && plannedCount > 0
                && (copyableCount > 0 || copiedCount > 0 || skippedCount > 0 || blockedCount > 0);
    }

    private static boolean noop(
            boolean passed,
            int plannedCount,
            int copyableCount,
            int copiedCount) {
        return passed && plannedCount == 0 && copyableCount == 0 && copiedCount == 0;
    }

    private static String status(
            boolean passed,
            boolean planningOnly,
            boolean dryRun,
            boolean complete,
            boolean partial,
            boolean noop,
            int copyableCount,
            int copiedCount,
            int skippedCount,
            int blockedCount,
            int failedCount) {
        if (!passed || failedCount > 0) {
            return STATUS_FAILED;
        }
        if (planningOnly || dryRun) {
            if (copyableCount > 0) {
                return STATUS_PREVIEW;
            }
            if (blockedCount > 0) {
                return STATUS_BLOCKED;
            }
            if (skippedCount > 0) {
                return STATUS_SKIPPED;
            }
            return STATUS_NOOP;
        }
        if (complete) {
            return STATUS_COMPLETE;
        }
        if (partial || copiedCount > 0) {
            return STATUS_PARTIAL;
        }
        if (blockedCount > 0) {
            return STATUS_BLOCKED;
        }
        if (skippedCount > 0 || noop) {
            return STATUS_SKIPPED;
        }
        return STATUS_NOOP;
    }

    private static List<String> copyableDocumentIds(
            List<AgenticCommerceWayangPersistenceTransferDocumentStatus> documents) {
        return documents.stream()
                .filter(AgenticCommerceWayangPersistenceTransferDocumentStatus::copyable)
                .map(AgenticCommerceWayangPersistenceTransferDocumentStatus::id)
                .toList();
    }

    private static List<String> failedDocumentIds(
            List<AgenticCommerceWayangPersistenceTransferDocumentStatus> documents) {
        return documents.stream()
                .filter(AgenticCommerceWayangPersistenceTransferDocumentStatus::failed)
                .map(AgenticCommerceWayangPersistenceTransferDocumentStatus::id)
                .toList();
    }

    private static List<String> attentionReasons(
            boolean planningOnly,
            boolean dryRun,
            boolean passed,
            int copyableCount,
            int skippedCount,
            int blockedCount,
            int failedCount) {
        List<String> values = new java.util.ArrayList<>();
        addIf(values, !passed, "issues_present");
        addIf(values, failedCount > 0, "failed_documents");
        addIf(values, blockedCount > 0, "blocked_documents");
        addIf(values, skippedCount > 0, "skipped_documents");
        addIf(values, dryRun, "dry_run");
        addIf(values, planningOnly, "planning_only");
        addIf(values, passed && copyableCount == 0 && blockedCount > 0, "no_copyable_documents");
        return List.copyOf(values);
    }

    private static Map<String, Object> reportAttributes(
            AgenticCommerceWayangPersistenceTransferReport report) {
        AgenticCommerceWayangPersistenceTargetComparison comparison =
                report.persistenceTargetComparisonAfter();
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("sourceStoreKind", report.sourceStoreKind());
        values.put("targetStoreKind", report.targetStoreKind());
        values.put("sourcePersistenceTarget", report.sourcePersistenceTarget());
        values.put("targetPersistenceTarget", report.targetPersistenceTargetAfter());
        values.put("targetChangeReasons", comparison.changeReasons());
        return Map.copyOf(values);
    }

    private static Map<String, Object> planAttributes(
            AgenticCommerceWayangPersistenceTransferPlan plan) {
        AgenticCommerceWayangPersistenceTargetComparison comparison =
                plan.persistenceTargetComparison();
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("sourceStoreKind", plan.sourceStoreKind());
        values.put("targetStoreKind", plan.targetStoreKind());
        values.put("sourcePersistenceTarget", plan.sourcePersistenceTarget());
        values.put("targetPersistenceTarget", plan.targetPersistenceTarget());
        values.put("targetChangeReasons", comparison.changeReasons());
        return Map.copyOf(values);
    }

    private static void addIf(List<String> values, boolean condition, String value) {
        if (condition) {
            values.add(value);
        }
    }

    private static String normalizeStatus(String value) {
        String normalized = AgenticCommerceWayangMaps.text(value);
        if (STATUS_COMPLETE.equals(normalized)
                || STATUS_PREVIEW.equals(normalized)
                || STATUS_PARTIAL.equals(normalized)
                || STATUS_BLOCKED.equals(normalized)
                || STATUS_SKIPPED.equals(normalized)
                || STATUS_FAILED.equals(normalized)
                || STATUS_NOOP.equals(normalized)) {
            return normalized;
        }
        return STATUS_FAILED;
    }
}

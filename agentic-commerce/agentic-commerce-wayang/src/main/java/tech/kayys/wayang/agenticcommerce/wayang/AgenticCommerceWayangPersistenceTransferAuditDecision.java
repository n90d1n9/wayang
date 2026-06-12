package tech.kayys.wayang.agenticcommerce.wayang;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Automation-facing decision derived from a persistence transfer audit summary.
 */
public record AgenticCommerceWayangPersistenceTransferAuditDecision(
        String nextAction,
        String decisionStatus,
        boolean terminal,
        boolean targetMutationExpected,
        boolean forceRequired,
        boolean retryRecommended,
        boolean inspectionRecommended,
        boolean operatorApprovalRequired,
        boolean requiresAttention,
        List<String> reasons,
        List<String> recommendationActions,
        Map<String, Object> attributes) {

    public static final String ACTION_APPLY = "apply";
    public static final String ACTION_FORCE = "force";
    public static final String ACTION_RETRY = "retry";
    public static final String ACTION_INSPECT = "inspect";
    public static final String ACTION_STOP = "stop";

    public static final String STATUS_READY_TO_APPLY = "ready_to_apply";
    public static final String STATUS_PREVIEW_ONLY = "preview_only";
    public static final String STATUS_ATTENTION_REQUIRED = "attention_required";
    public static final String STATUS_FORCE_OR_CLEAR_REQUIRED = "force_or_clear_required";
    public static final String STATUS_FAILED_RETRYABLE = "failed_retryable";
    public static final String STATUS_COMPLETE = "complete";
    public static final String STATUS_FORCED_COMPLETE = "forced_complete";
    public static final String STATUS_NOOP = "noop";

    public AgenticCommerceWayangPersistenceTransferAuditDecision {
        nextAction = AgenticCommerceWayangMaps.required(nextAction, "nextAction");
        decisionStatus = AgenticCommerceWayangMaps.required(decisionStatus, "decisionStatus");
        reasons = AgenticCommerceWayangMaps.stringList(reasons);
        recommendationActions = AgenticCommerceWayangMaps.stringList(recommendationActions);
        attributes = AgenticCommerceWayangMaps.copy(attributes);
    }

    public static AgenticCommerceWayangPersistenceTransferAuditDecision from(
            AgenticCommerceWayangPersistenceTransferAuditSummary summary) {
        AgenticCommerceWayangPersistenceTransferAuditSummary resolved =
                Objects.requireNonNull(summary, "summary");
        String outcome = resolved.outcomeStatus();
        String nextAction = nextAction(outcome);
        String decisionStatus = decisionStatus(outcome);
        boolean blocked = AgenticCommerceWayangPersistenceTransferAuditSummary.OUTCOME_BLOCKED.equals(outcome);
        boolean failed = AgenticCommerceWayangPersistenceTransferAuditSummary.OUTCOME_FAILED.equals(outcome);
        boolean preview = AgenticCommerceWayangPersistenceTransferAuditSummary.OUTCOME_PREVIEW.equals(outcome);
        boolean attention = AgenticCommerceWayangPersistenceTransferAuditSummary.OUTCOME_ATTENTION.equals(outcome);
        boolean forced = AgenticCommerceWayangPersistenceTransferAuditSummary.OUTCOME_FORCED.equals(outcome);
        boolean complete = AgenticCommerceWayangPersistenceTransferAuditSummary.OUTCOME_COMPLETE.equals(outcome);
        boolean noop = AgenticCommerceWayangPersistenceTransferAuditSummary.OUTCOME_NOOP.equals(outcome);
        return new AgenticCommerceWayangPersistenceTransferAuditDecision(
                nextAction,
                decisionStatus,
                complete || forced || noop,
                targetMutationExpected(resolved, nextAction),
                blocked,
                failed,
                preview || attention || blocked || failed || forced || resolved.requiresAttention(),
                blocked || attention || forced,
                resolved.requiresAttention(),
                reasons(resolved),
                resolved.recommendationActions(),
                attributes(resolved));
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("nextAction", nextAction);
        values.put("decisionStatus", decisionStatus);
        values.put("terminal", terminal);
        values.put("targetMutationExpected", targetMutationExpected);
        values.put("forceRequired", forceRequired);
        values.put("retryRecommended", retryRecommended);
        values.put("inspectionRecommended", inspectionRecommended);
        values.put("operatorApprovalRequired", operatorApprovalRequired);
        values.put("requiresAttention", requiresAttention);
        values.put("reasons", reasons);
        values.put("recommendationActions", recommendationActions);
        values.put("attributes", attributes);
        return Map.copyOf(values);
    }

    private static String nextAction(String outcomeStatus) {
        return switch (outcomeStatus) {
            case AgenticCommerceWayangPersistenceTransferAuditSummary.OUTCOME_READY -> ACTION_APPLY;
            case AgenticCommerceWayangPersistenceTransferAuditSummary.OUTCOME_BLOCKED -> ACTION_FORCE;
            case AgenticCommerceWayangPersistenceTransferAuditSummary.OUTCOME_FAILED -> ACTION_RETRY;
            case AgenticCommerceWayangPersistenceTransferAuditSummary.OUTCOME_PREVIEW,
                    AgenticCommerceWayangPersistenceTransferAuditSummary.OUTCOME_ATTENTION -> ACTION_INSPECT;
            default -> ACTION_STOP;
        };
    }

    private static String decisionStatus(String outcomeStatus) {
        return switch (outcomeStatus) {
            case AgenticCommerceWayangPersistenceTransferAuditSummary.OUTCOME_READY -> STATUS_READY_TO_APPLY;
            case AgenticCommerceWayangPersistenceTransferAuditSummary.OUTCOME_PREVIEW -> STATUS_PREVIEW_ONLY;
            case AgenticCommerceWayangPersistenceTransferAuditSummary.OUTCOME_ATTENTION -> STATUS_ATTENTION_REQUIRED;
            case AgenticCommerceWayangPersistenceTransferAuditSummary.OUTCOME_BLOCKED -> STATUS_FORCE_OR_CLEAR_REQUIRED;
            case AgenticCommerceWayangPersistenceTransferAuditSummary.OUTCOME_FAILED -> STATUS_FAILED_RETRYABLE;
            case AgenticCommerceWayangPersistenceTransferAuditSummary.OUTCOME_FORCED -> STATUS_FORCED_COMPLETE;
            case AgenticCommerceWayangPersistenceTransferAuditSummary.OUTCOME_NOOP -> STATUS_NOOP;
            default -> STATUS_COMPLETE;
        };
    }

    private static boolean targetMutationExpected(
            AgenticCommerceWayangPersistenceTransferAuditSummary summary,
            String nextAction) {
        return ACTION_APPLY.equals(nextAction) || ACTION_FORCE.equals(nextAction)
                ? summary.copyableDocumentCount() > 0
                : false;
    }

    private static List<String> reasons(AgenticCommerceWayangPersistenceTransferAuditSummary summary) {
        List<String> values = new ArrayList<>();
        values.add(summary.outcomeStatus());
        if (!summary.successful()) {
            values.add("not_successful");
        }
        if (summary.blocked()) {
            values.add("blocked");
        }
        if (summary.forced()) {
            values.add("forced");
        }
        if (summary.dryRun()) {
            values.add("dry_run");
        }
        if (summary.issueCount() > 0) {
            values.add("issues_present");
        }
        if (summary.warningCount() > 0) {
            values.add("warnings_present");
        }
        values.addAll(summary.attentionReasons());
        return AgenticCommerceWayangMaps.stringList(values);
    }

    private static Map<String, Object> attributes(
            AgenticCommerceWayangPersistenceTransferAuditSummary summary) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("trailType", summary.trailType());
        values.put("trailStatus", summary.trailStatus());
        values.put("outcomeStatus", summary.outcomeStatus());
        values.put("eventCount", summary.eventCount());
        values.put("plannedDocumentCount", summary.plannedDocumentCount());
        values.put("copyableDocumentCount", summary.copyableDocumentCount());
        values.put("copiedDocumentCount", summary.copiedDocumentCount());
        values.put("blockedDocumentCount", summary.blockedDocumentCount());
        values.put("operatorActionRecommended", summary.operatorActionRecommended());
        return Map.copyOf(values);
    }
}

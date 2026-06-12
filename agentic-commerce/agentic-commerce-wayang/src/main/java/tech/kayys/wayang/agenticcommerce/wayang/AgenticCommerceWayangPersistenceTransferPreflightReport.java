package tech.kayys.wayang.agenticcommerce.wayang;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Non-mutating readiness report for a persistence transfer.
 */
public record AgenticCommerceWayangPersistenceTransferPreflightReport(
        AgenticCommerceWayangPersistenceTransferOptions options,
        AgenticCommerceWayangPersistenceHealthReport sourceHealth,
        AgenticCommerceWayangPersistenceHealthReport targetHealth,
        AgenticCommerceWayangPersistenceTransferPlan plan,
        Map<String, Object> attributes) {

    public static final String STATUS_READY = "ready";
    public static final String STATUS_BLOCKED = "blocked";
    public static final String STATUS_SOURCE_INCOMPLETE = "source_incomplete";
    public static final String STATUS_SOURCE_UNAVAILABLE = "source_unavailable";
    public static final String STATUS_TARGET_UNAVAILABLE = "target_unavailable";
    public static final String STATUS_PLAN_FAILED = "plan_failed";
    public static final String STATUS_NOOP = "noop";

    public AgenticCommerceWayangPersistenceTransferPreflightReport {
        options = options == null ? AgenticCommerceWayangPersistenceTransferOptions.defaults() : options;
        sourceHealth = Objects.requireNonNull(sourceHealth, "sourceHealth");
        targetHealth = Objects.requireNonNull(targetHealth, "targetHealth");
        plan = Objects.requireNonNull(plan, "plan");
        attributes = AgenticCommerceWayangMaps.copy(attributes);
    }

    public static AgenticCommerceWayangPersistenceTransferPreflightReport from(
            AgenticCommerceWayangPersistenceStore source,
            AgenticCommerceWayangPersistenceStore target,
            AgenticCommerceWayangPersistenceTransferOptions transferOptions) {
        AgenticCommerceWayangPersistenceStore resolvedSource = Objects.requireNonNull(source, "source");
        AgenticCommerceWayangPersistenceStore resolvedTarget = Objects.requireNonNull(target, "target");
        AgenticCommerceWayangPersistenceTransferOptions options = transferOptions == null
                ? AgenticCommerceWayangPersistenceTransferOptions.defaults()
                : transferOptions;
        AgenticCommerceWayangPersistenceHealthReport sourceHealth =
                AgenticCommerceWayangPersistenceHealthReport.from(resolvedSource);
        AgenticCommerceWayangPersistenceHealthReport targetHealth =
                AgenticCommerceWayangPersistenceHealthReport.from(resolvedTarget);
        AgenticCommerceWayangPersistenceTransferPlan plan =
                AgenticCommerceWayangPersistenceTransfer.configured(options).plan(resolvedSource, resolvedTarget);
        return new AgenticCommerceWayangPersistenceTransferPreflightReport(
                options,
                sourceHealth,
                targetHealth,
                plan,
                attributes(resolvedSource, resolvedTarget, options));
    }

    public boolean passed() {
        return sourceHealth.ready() && targetHealth.ready() && plan.passed();
    }

    public boolean sourceReady() {
        return sourceHealth.ready() && sourceHealth.complete();
    }

    public boolean targetReady() {
        return targetHealth.ready();
    }

    public boolean blocked() {
        return plan.blockedDocumentCount() > 0;
    }

    public boolean wouldMutateTarget() {
        return plan.wouldMutateTarget();
    }

    public boolean readyToApply() {
        return passed() && sourceReady() && targetReady() && wouldMutateTarget() && !blocked();
    }

    public String preflightStatus() {
        if (!sourceHealth.ready()) {
            return STATUS_SOURCE_UNAVAILABLE;
        }
        if (!targetHealth.ready()) {
            return STATUS_TARGET_UNAVAILABLE;
        }
        if (!plan.passed()) {
            return STATUS_PLAN_FAILED;
        }
        if (!sourceHealth.complete()) {
            return STATUS_SOURCE_INCOMPLETE;
        }
        if (blocked()) {
            return STATUS_BLOCKED;
        }
        if (wouldMutateTarget()) {
            return STATUS_READY;
        }
        return STATUS_NOOP;
    }

    public int issueCount() {
        return sourceHealth.issueCount() + targetHealth.issueCount() + plan.issueCount();
    }

    public int warningCount() {
        return sourceHealth.warningCount() + targetHealth.warningCount() + plan.warningFindingCount();
    }

    public int findingCount() {
        return sourceHealth.findingCount() + targetHealth.findingCount() + plan.findingCount();
    }

    public List<AgenticCommerceWayangPersistenceTransferPreflightCheck> checks() {
        return List.of(
                AgenticCommerceWayangPersistenceTransferPreflightCheck.sourceHealth(sourceHealth),
                AgenticCommerceWayangPersistenceTransferPreflightCheck.targetHealth(targetHealth),
                AgenticCommerceWayangPersistenceTransferPreflightCheck.transferPlan(plan));
    }

    public List<AgenticCommerceWayangPersistenceTransferPreflightCheck> readyChecks() {
        return checks().stream()
                .filter(AgenticCommerceWayangPersistenceTransferPreflightCheck::ready)
                .toList();
    }

    public List<AgenticCommerceWayangPersistenceTransferPreflightCheck> blockingChecks() {
        return checks().stream()
                .filter(AgenticCommerceWayangPersistenceTransferPreflightCheck::blocking)
                .toList();
    }

    public int checkCount() {
        return checks().size();
    }

    public int readyCheckCount() {
        return readyChecks().size();
    }

    public int blockingCheckCount() {
        return blockingChecks().size();
    }

    public List<String> attentionReasons() {
        return checks().stream()
                .flatMap(check -> check.attentionReasons().stream())
                .distinct()
                .toList();
    }

    public List<AgenticCommerceWayangPersistenceTransferPreflightRecommendation> recommendations() {
        return AgenticCommerceWayangPersistenceTransferPreflightRecommendation.from(this);
    }

    public List<AgenticCommerceWayangPersistenceTransferPreflightRecommendation> blockingRecommendations() {
        return recommendations().stream()
                .filter(AgenticCommerceWayangPersistenceTransferPreflightRecommendation::blocking)
                .toList();
    }

    public int recommendationCount() {
        return recommendations().size();
    }

    public int blockingRecommendationCount() {
        return blockingRecommendations().size();
    }

    public List<String> recommendationActions() {
        return recommendations().stream()
                .map(AgenticCommerceWayangPersistenceTransferPreflightRecommendation::action)
                .toList();
    }

    public AgenticCommerceWayangPersistenceTransferAuditEvent auditEvent() {
        return AgenticCommerceWayangPersistenceTransferAuditEvent.from(this);
    }

    public AgenticCommerceWayangPersistenceTransferAuditTrail auditTrail() {
        return AgenticCommerceWayangPersistenceTransferAuditTrail.from(this);
    }

    public Map<String, Object> toMap() {
        List<AgenticCommerceWayangPersistenceTransferPreflightCheck> checks = checks();
        List<AgenticCommerceWayangPersistenceTransferPreflightRecommendation> recommendations =
                recommendations();
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("preflightStatus", preflightStatus());
        values.put("passed", passed());
        values.put("readyToApply", readyToApply());
        values.put("sourceReady", sourceReady());
        values.put("targetReady", targetReady());
        values.put("blocked", blocked());
        values.put("wouldMutateTarget", wouldMutateTarget());
        values.put("issueCount", issueCount());
        values.put("warningCount", warningCount());
        values.put("findingCount", findingCount());
        values.put("checkCount", checks.size());
        values.put("readyCheckCount", readyCheckCount());
        values.put("blockingCheckCount", blockingCheckCount());
        values.put("recommendationCount", recommendations.size());
        values.put("blockingRecommendationCount", blockingRecommendationCount());
        values.put("recommendationActions", recommendations.stream()
                .map(AgenticCommerceWayangPersistenceTransferPreflightRecommendation::action)
                .toList());
        values.put("attentionReasons", attentionReasons());
        values.put("checks", checks.stream()
                .map(AgenticCommerceWayangPersistenceTransferPreflightCheck::toMap)
                .toList());
        values.put("checksById", checksById(checks));
        values.put("recommendations", recommendations.stream()
                .map(AgenticCommerceWayangPersistenceTransferPreflightRecommendation::toMap)
                .toList());
        values.put("auditEvent", auditEvent().toMap());
        values.put("auditTrail", auditTrail().toMap());
        values.put("options", options.toMap());
        values.put("sourceHealthStatus", sourceHealth.healthStatus());
        values.put("targetHealthStatus", targetHealth.healthStatus());
        values.put("sourceHealthSummary", sourceHealth.summary().toMap());
        values.put("targetHealthSummary", targetHealth.summary().toMap());
        values.put("sourceMissingDocumentIds", sourceHealth.documentIndex().missingIds());
        values.put("sourceFailedDocumentIds", sourceHealth.documentIndex().failedIds());
        values.put("targetMissingDocumentIds", targetHealth.documentIndex().missingIds());
        values.put("targetFailedDocumentIds", targetHealth.documentIndex().failedIds());
        values.put("planSummary", plan.summary().toMap());
        values.put("planFindingIndex", plan.findingIndex().toMap());
        values.put("plan", plan.toMap());
        values.put("sourceHealth", sourceHealth.toMap());
        values.put("targetHealth", targetHealth.toMap());
        values.put("sourcePersistenceTarget", sourceHealth.persistenceTarget());
        values.put("targetPersistenceTarget", targetHealth.persistenceTarget());
        values.put("persistenceTargetComparison", plan.persistenceTargetComparison().toMap());
        values.put("attributes", attributes);
        return Map.copyOf(values);
    }

    private static Map<String, Object> checksById(
            List<AgenticCommerceWayangPersistenceTransferPreflightCheck> checks) {
        Map<String, Object> values = new LinkedHashMap<>();
        checks.forEach(check -> values.put(check.checkId(), check.toMap()));
        return Map.copyOf(values);
    }

    private static Map<String, Object> attributes(
            AgenticCommerceWayangPersistenceStore source,
            AgenticCommerceWayangPersistenceStore target,
            AgenticCommerceWayangPersistenceTransferOptions options) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("preflightId", "agentic-commerce-wayang-persistence-transfer-preflight");
        values.put("sourceStoreKind", source.storageKind());
        values.put("targetStoreKind", target.storageKind());
        values.put("dryRun", options.dryRun());
        values.put("overwriteExisting", options.overwriteExisting());
        values.put("verifyAfterCopy", options.verifyAfterCopy());
        return Map.copyOf(values);
    }

}

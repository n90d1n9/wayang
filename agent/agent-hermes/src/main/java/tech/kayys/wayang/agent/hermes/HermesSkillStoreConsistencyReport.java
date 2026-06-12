package tech.kayys.wayang.agent.hermes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Dry-run consistency view for learned-skill stores and lineage indexes.
 */
public record HermesSkillStoreConsistencyReport(
        String status,
        boolean consistent,
        boolean attentionRequired,
        int issueCount,
        int criticalIssueCount,
        int warningIssueCount,
        int advisoryIssueCount,
        int learnedSkillCount,
        int rootCount,
        long orphanedRootCount,
        List<HermesSkillStoreConsistencyIssue> issues) {

    public HermesSkillStoreConsistencyReport {
        issues = HermesCollections.copyNonNull(issues);
        issueCount = issues.size();
        criticalIssueCount = (int) issues.stream()
                .filter(issue -> "critical".equals(issue.severity()))
                .count();
        warningIssueCount = (int) issues.stream()
                .filter(issue -> "warning".equals(issue.severity()))
                .count();
        advisoryIssueCount = issueCount - criticalIssueCount - warningIssueCount;
        attentionRequired = attentionRequired || issues.stream()
                .anyMatch(HermesSkillStoreConsistencyIssue::attentionRequired);
        consistent = consistent && !attentionRequired && criticalIssueCount == 0 && warningIssueCount == 0;
        learnedSkillCount = Math.max(learnedSkillCount, 0);
        rootCount = Math.max(rootCount, 0);
        orphanedRootCount = Math.max(orphanedRootCount, 0);
        status = HermesText.oneLineOr(status, defaultStatus(consistent, attentionRequired, issueCount, learnedSkillCount));
    }

    public static HermesSkillStoreConsistencyReport empty() {
        return from(HermesSkillLineageCatalog.empty());
    }

    public static HermesSkillStoreConsistencyReport from(HermesSkillLineageCatalog catalog) {
        HermesSkillLineageCatalog resolved = catalog == null ? HermesSkillLineageCatalog.empty() : catalog;
        HermesSkillLineageHealth health = resolved.health();
        HermesSkillLineageRemediationPlan remediationPlan = health.remediationPlan();
        List<HermesSkillStoreConsistencyIssue> issues = remediationPlan.actions().stream()
                .map(HermesSkillStoreConsistencyIssue::from)
                .toList();
        return new HermesSkillStoreConsistencyReport(
                status(health),
                !health.attentionRequired() && resolved.orphanedRootCount() == 0,
                health.attentionRequired(),
                0,
                0,
                0,
                0,
                resolved.learnedSkillCount(),
                resolved.rootCount(),
                resolved.orphanedRootCount(),
                issues);
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("status", status);
        values.put("consistent", consistent);
        values.put("attentionRequired", attentionRequired);
        values.put("issueCount", issueCount);
        values.put("criticalIssueCount", criticalIssueCount);
        values.put("warningIssueCount", warningIssueCount);
        values.put("advisoryIssueCount", advisoryIssueCount);
        values.put("learnedSkillCount", learnedSkillCount);
        values.put("rootCount", rootCount);
        values.put("orphanedRootCount", orphanedRootCount);
        values.put("issues", issues.stream()
                .map(HermesSkillStoreConsistencyIssue::toMetadata)
                .toList());
        return Map.copyOf(values);
    }

    private static String status(HermesSkillLineageHealth health) {
        if (health == null) {
            return "empty";
        }
        return switch (health.status()) {
            case "attention" -> "inconsistent";
            case "evolving" -> "advisory";
            case "empty" -> "empty";
            default -> "consistent";
        };
    }

    private static String defaultStatus(boolean consistent, boolean attentionRequired, int issueCount,
            int learnedSkillCount) {
        if (attentionRequired) {
            return "inconsistent";
        }
        if (learnedSkillCount == 0) {
            return "empty";
        }
        return consistent && issueCount == 0 ? "consistent" : "advisory";
    }
}

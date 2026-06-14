package tech.kayys.wayang.gollek.sdk;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Dry-run report describing how run-store retention compaction would affect the current snapshot.
 */
public record AgentRunStoreCompactionPreview(
        boolean dryRun,
        AgentRunStoreDiagnostics diagnostics,
        AgentRunStoreBackupRetentionResult backupRetention,
        List<AgentRunStoreVerificationIssue> issues) {

    public AgentRunStoreCompactionPreview(
            boolean dryRun,
            AgentRunStoreDiagnostics diagnostics,
            List<AgentRunStoreVerificationIssue> issues) {
        this(dryRun, diagnostics, null, issues);
    }

    public AgentRunStoreCompactionPreview {
        diagnostics = diagnostics == null
                ? AgentRunStore.memory().diagnostics()
                : diagnostics;
        backupRetention = backupRetention == null
                ? AgentRunStoreBackupRetentionResult.preview(
                        diagnostics.backupRetentionPolicy(),
                        diagnostics.backupInventory())
                : backupRetention;
        issues = issues == null ? List.of() : List.copyOf(issues);
    }

    public boolean previewable() {
        return errorCount() == 0;
    }

    public boolean compactionNeeded() {
        return previewable() && retentionAssessment().pruned();
    }

    public int exitCode() {
        return previewable() ? 0 : 1;
    }

    public int issueCount() {
        return issues.size();
    }

    public int errorCount() {
        return (int) issues.stream()
                .filter(AgentRunStoreVerificationIssue::error)
                .count();
    }

    public int warningCount() {
        return (int) issues.stream()
                .filter(AgentRunStoreVerificationIssue::warning)
                .count();
    }

    public AgentRunStoreRetentionAssessment retentionAssessment() {
        return diagnostics.retentionAssessment();
    }

    public String message() {
        if (!previewable()) {
            return "Run-store compaction preview could not be prepared.";
        }
        if (!compactionNeeded()) {
            return "Run-store compaction is not needed.";
        }
        AgentRunStoreRetentionAssessment assessment = retentionAssessment();
        return "Run-store compaction would prune "
                + assessment.prunedRuns()
                + " runs, "
                + assessment.prunedStatuses()
                + " statuses, and "
                + assessment.prunedEvents()
                + " events.";
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("dryRun", dryRun);
        values.put("previewable", previewable());
        values.put("compactionNeeded", compactionNeeded());
        values.put("exitCode", exitCode());
        values.put("issueCount", issueCount());
        values.put("errorCount", errorCount());
        values.put("warningCount", warningCount());
        values.put("issues", issues.stream()
                .map(AgentRunStoreVerificationIssue::toMap)
                .toList());
        values.put("message", message());
        values.put("retentionAssessment", retentionAssessment().toMap());
        values.put("backupRetention", backupRetention.toMap());
        values.put("diagnostics", diagnostics.toMap());
        return AgentRunEnvelopeMaps.copy(values);
    }
}

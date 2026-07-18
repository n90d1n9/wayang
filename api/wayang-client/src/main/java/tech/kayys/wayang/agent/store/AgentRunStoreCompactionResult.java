package tech.kayys.wayang.agent.store;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import tech.kayys.wayang.agent.run.AgentRunEnvelopeMaps;
import tech.kayys.wayang.client.SdkText;

/**
 * Result of an explicit run-store retention compaction attempt.
 */
public record AgentRunStoreCompactionResult(
        boolean applied,
        boolean compacted,
        boolean backupCreated,
        String backupPath,
        AgentRunStoreBackupRetentionResult backupRetention,
        AgentRunStoreDiagnostics beforeDiagnostics,
        AgentRunStoreDiagnostics afterDiagnostics,
        List<AgentRunStoreVerificationIssue> issues) {

    public AgentRunStoreCompactionResult {
        backupPath = SdkText.trimToEmpty(backupPath);
        backupRetention = backupRetention == null
                ? AgentRunStoreBackupRetentionResult.empty(AgentRunStoreBackupRetentionPolicy.defaults())
                : backupRetention;
        beforeDiagnostics = beforeDiagnostics == null
                ? AgentRunStore.memory().diagnostics()
                : beforeDiagnostics;
        afterDiagnostics = afterDiagnostics == null ? beforeDiagnostics : afterDiagnostics;
        issues = issues == null ? List.of() : List.copyOf(issues);
    }

    public boolean successful() {
        return errorCount() == 0;
    }

    public boolean compactionNeeded() {
        return successful() && beforeDiagnostics.retentionAssessment().pruned();
    }

    public int exitCode() {
        return successful() ? 0 : 1;
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
        return beforeDiagnostics.retentionAssessment();
    }

    public String message() {
        if (!successful()) {
            return "Run-store compaction could not be applied.";
        }
        if (!compactionNeeded()) {
            return "Run-store compaction was not needed.";
        }
        AgentRunStoreRetentionAssessment assessment = retentionAssessment();
        return "Run-store compaction pruned "
                + assessment.prunedRuns()
                + " runs, "
                + assessment.prunedStatuses()
                + " statuses, and "
                + assessment.prunedEvents()
                + " events.";
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("applied", applied);
        values.put("compacted", compacted);
        values.put("backupCreated", backupCreated);
        values.put("backupPath", backupPath);
        values.put("backupRetention", backupRetention.toMap());
        values.put("successful", successful());
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
        values.put("beforeDiagnostics", beforeDiagnostics.toMap());
        values.put("afterDiagnostics", afterDiagnostics.toMap());
        return AgentRunEnvelopeMaps.copy(values);
    }
}

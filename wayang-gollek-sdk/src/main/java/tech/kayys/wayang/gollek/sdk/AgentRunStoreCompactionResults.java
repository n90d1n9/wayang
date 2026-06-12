package tech.kayys.wayang.gollek.sdk;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory methods for explicit run-store compaction results.
 */
final class AgentRunStoreCompactionResults {

    private AgentRunStoreCompactionResults() {
    }

    static AgentRunStoreCompactionResult skipped(AgentRunStoreVerification verification) {
        return skipped(verification, AgentRunStoreBackupRetentionPolicy.defaults());
    }

    static AgentRunStoreCompactionResult skipped(
            AgentRunStoreVerification verification,
            AgentRunStoreBackupRetentionPolicy backupRetentionPolicy) {
        AgentRunStoreVerification model = verification == null
                ? AgentRunStore.memory().verification()
                : verification;
        return new AgentRunStoreCompactionResult(
                false,
                false,
                false,
                "",
                AgentRunStoreBackupRetentionResult.empty(backupRetentionPolicy),
                model.diagnostics(),
                model.diagnostics(),
                model.issues());
    }

    static AgentRunStoreCompactionResult compacted(
            AgentRunStoreVerification before,
            AgentRunStoreDiagnostics after,
            String backupPath,
            AgentRunStoreBackupRetentionResult backupRetention) {
        AgentRunStoreVerification model = before == null
                ? AgentRunStore.memory().verification()
                : before;
        List<AgentRunStoreVerificationIssue> issues = new ArrayList<>(model.issues());
        addBackupRetentionFailureWarning(backupRetention, issues);
        return new AgentRunStoreCompactionResult(
                true,
                true,
                !SdkText.trimToEmpty(backupPath).isEmpty(),
                backupPath,
                backupRetention,
                model.diagnostics(),
                after,
                issues);
    }

    static AgentRunStoreCompactionResult failed(
            AgentRunStoreVerification before,
            AgentRunStoreBackupRetentionPolicy backupRetentionPolicy,
            AgentRunStoreVerificationIssue issue) {
        AgentRunStoreVerification model = before == null
                ? AgentRunStore.memory().verification()
                : before;
        List<AgentRunStoreVerificationIssue> issues = new ArrayList<>(model.issues());
        if (issue != null) {
            issues.add(issue);
        }
        return new AgentRunStoreCompactionResult(
                false,
                false,
                false,
                "",
                AgentRunStoreBackupRetentionResult.empty(backupRetentionPolicy),
                model.diagnostics(),
                model.diagnostics(),
                issues);
    }

    private static void addBackupRetentionFailureWarning(
            AgentRunStoreBackupRetentionResult backupRetention,
            List<AgentRunStoreVerificationIssue> issues) {
        if (backupRetention == null || backupRetention.failedBackupPruneCount() <= 0) {
            return;
        }
        issues.add(AgentRunStoreVerificationIssue.warning(
                "backup-retention.prune-incomplete",
                "Run-store compaction completed, but "
                        + backupRetention.failedBackupPruneCount()
                        + " old backup snapshots could not be pruned."));
    }
}

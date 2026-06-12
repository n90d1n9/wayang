package tech.kayys.wayang.gollek.sdk;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared verification policy for run-store diagnostics across memory, custom,
 * and file-backed implementations.
 */
final class AgentRunStoreVerifications {

    private AgentRunStoreVerifications() {
    }

    static AgentRunStoreVerification fromDiagnostics(AgentRunStoreDiagnostics diagnostics) {
        AgentRunStoreDiagnostics model = diagnostics == null
                ? AgentRunStore.memory().diagnostics()
                : diagnostics;
        List<AgentRunStoreVerificationIssue> issues = new ArrayList<>();
        if (model.unsupportedSnapshotVersion()) {
            issues.add(AgentRunStoreVerificationIssue.error(
                    "snapshot.unsupported-version",
                    "Run-store snapshot version is not supported by this SDK."));
        }
        addRetentionWarning(model, issues);
        addBackupRetentionWarning(model, issues);
        return new AgentRunStoreVerification(model, issues);
    }

    static void addRetentionWarning(
            AgentRunStoreDiagnostics diagnostics,
            List<AgentRunStoreVerificationIssue> issues) {
        if (diagnostics == null
                || diagnostics.retentionAssessment() == null
                || !diagnostics.retentionAssessment().pruned()) {
            return;
        }
        issues.add(AgentRunStoreVerificationIssue.warning(
                "retention.would-prune",
                "Run-store snapshot exceeds the configured retention policy and will be compacted on write."));
    }

    static void addBackupRetentionWarning(
            AgentRunStoreDiagnostics diagnostics,
            List<AgentRunStoreVerificationIssue> issues) {
        if (diagnostics == null
                || diagnostics.backupRetentionPolicy() == null
                || diagnostics.backupRetentionPolicy().isUnlimited()
                || diagnostics.backupInventory() == null) {
            return;
        }
        int prunableBackups = diagnostics.backupInventory().backupCount()
                - diagnostics.backupRetentionPolicy().maxBackups();
        if (prunableBackups <= 0) {
            return;
        }
        issues.add(AgentRunStoreVerificationIssue.warning(
                "backup-retention.would-prune",
                "Run-store compaction backups exceed the configured backup retention policy; "
                        + prunableBackups
                        + " old backups will be pruned after the next successful compaction."));
    }
}

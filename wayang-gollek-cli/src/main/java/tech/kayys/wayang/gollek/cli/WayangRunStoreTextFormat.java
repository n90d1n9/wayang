package tech.kayys.wayang.gollek.cli;

import tech.kayys.wayang.gollek.sdk.AgentRunStoreDiagnostics;
import tech.kayys.wayang.gollek.sdk.AgentRunStoreBackupInventory;
import tech.kayys.wayang.gollek.sdk.AgentRunStoreBackupRetentionResult;
import tech.kayys.wayang.gollek.sdk.AgentRunStoreBackupRetentionPolicy;
import tech.kayys.wayang.gollek.sdk.AgentRunStoreCompactionPreview;
import tech.kayys.wayang.gollek.sdk.AgentRunStoreCompactionResult;
import tech.kayys.wayang.gollek.sdk.AgentRunStoreRetentionAssessment;
import tech.kayys.wayang.gollek.sdk.AgentRunStoreRetentionPolicy;
import tech.kayys.wayang.gollek.sdk.AgentRunStoreVerification;
import tech.kayys.wayang.gollek.sdk.AgentRunStoreVerificationIssue;
import tech.kayys.wayang.gollek.sdk.AgentRunStoreVerificationPolicy;

/**
 * Plain-text renderer for run-store diagnostics.
 */
final class WayangRunStoreTextFormat {

    private static final String NL = System.lineSeparator();

    private WayangRunStoreTextFormat() {
    }

    static String text(AgentRunStoreDiagnostics diagnostics) {
        AgentRunStoreDiagnostics model = diagnostics == null
                ? new AgentRunStoreDiagnostics(
                        "unknown",
                        false,
                        "",
                        "",
                        false,
                        false,
                        "",
                        false,
                        0,
                        0,
                        0,
                        AgentRunStoreRetentionPolicy.unlimited(),
                        null,
                        null,
                        null)
                : diagnostics;
        StringBuilder output = new StringBuilder("Wayang run store").append(NL);
        output.append("backend: ").append(model.backend()).append(NL);
        output.append("persistent: ").append(CliText.yesNo(model.persistent())).append(NL);
        appendPath(output, "path", model.path());
        appendPath(output, "lockPath", model.lockPath());
        output.append("snapshotPresent: ").append(CliText.yesNo(model.snapshotPresent())).append(NL);
        output.append("lockPresent: ").append(CliText.yesNo(model.lockPresent())).append(NL);
        appendPath(output, "snapshotVersion", model.snapshotVersion());
        output.append("unsupportedSnapshotVersion: ")
                .append(CliText.yesNo(model.unsupportedSnapshotVersion()))
                .append(NL);
        output.append("runs: ").append(model.runCount()).append(NL);
        output.append("statuses: ").append(model.statusCount()).append(NL);
        output.append("events: ").append(model.eventCount()).append(NL);
        appendRetention(output, model.retentionPolicy());
        appendAssessment(output, model.retentionAssessment());
        appendBackupRetentionPolicy(output, model.backupRetentionPolicy());
        appendBackupInventory(output, model.backupInventory());
        return output.toString();
    }

    static String verification(AgentRunStoreVerification verification) {
        return verification(verification, AgentRunStoreVerificationPolicy.lenient());
    }

    static String verification(
            AgentRunStoreVerification verification,
            AgentRunStoreVerificationPolicy policy) {
        AgentRunStoreVerification model = verification == null
                ? new AgentRunStoreVerification(null, java.util.List.of())
                : verification;
        AgentRunStoreVerificationPolicy resolvedPolicy = policy == null
                ? AgentRunStoreVerificationPolicy.lenient()
                : policy;
        StringBuilder output = new StringBuilder("Wayang run store verification").append(NL);
        output.append("passed: ").append(CliText.yesNo(model.passed(resolvedPolicy))).append(NL);
        output.append("exitCode: ").append(model.exitCode(resolvedPolicy)).append(NL);
        output.append("policy: ").append(resolvedPolicy.mode()).append(NL);
        output.append("errors: ").append(model.errorCount()).append(NL);
        output.append("warnings: ").append(model.warningCount()).append(NL);
        output.append("issues: ").append(model.issueCount()).append(NL);
        for (AgentRunStoreVerificationIssue issue : model.issues()) {
            output.append("- ")
                    .append(issue.severity())
                    .append(" ")
                    .append(issue.code())
                    .append(": ")
                    .append(issue.message())
                    .append(NL);
        }
        output.append(text(model.diagnostics()));
        return output.toString();
    }

    static String compactionPreview(AgentRunStoreCompactionPreview preview) {
        AgentRunStoreCompactionPreview model = preview == null
                ? new AgentRunStoreCompactionPreview(true, null, java.util.List.of())
                : preview;
        StringBuilder output = new StringBuilder("Wayang run store compaction preview").append(NL);
        output.append("dryRun: ").append(CliText.yesNo(model.dryRun())).append(NL);
        output.append("previewable: ").append(CliText.yesNo(model.previewable())).append(NL);
        output.append("compactionNeeded: ").append(CliText.yesNo(model.compactionNeeded())).append(NL);
        output.append("exitCode: ").append(model.exitCode()).append(NL);
        output.append("errors: ").append(model.errorCount()).append(NL);
        output.append("warnings: ").append(model.warningCount()).append(NL);
        output.append("issues: ").append(model.issueCount()).append(NL);
        output.append("message: ").append(model.message()).append(NL);
        appendAssessment(output, model.retentionAssessment());
        appendBackupRetention(output, model.backupRetention());
        output.append(text(model.diagnostics()));
        return output.toString();
    }

    static String compactionResult(AgentRunStoreCompactionResult result) {
        AgentRunStoreCompactionResult model = result == null
                ? new AgentRunStoreCompactionResult(false, false, false, "", null, null, null, java.util.List.of())
                : result;
        StringBuilder output = new StringBuilder("Wayang run store compaction").append(NL);
        output.append("applied: ").append(CliText.yesNo(model.applied())).append(NL);
        output.append("compacted: ").append(CliText.yesNo(model.compacted())).append(NL);
        output.append("backupCreated: ").append(CliText.yesNo(model.backupCreated())).append(NL);
        appendPath(output, "backupPath", model.backupPath());
        appendBackupRetention(output, model.backupRetention());
        output.append("successful: ").append(CliText.yesNo(model.successful())).append(NL);
        output.append("compactionNeeded: ").append(CliText.yesNo(model.compactionNeeded())).append(NL);
        output.append("exitCode: ").append(model.exitCode()).append(NL);
        output.append("errors: ").append(model.errorCount()).append(NL);
        output.append("warnings: ").append(model.warningCount()).append(NL);
        output.append("issues: ").append(model.issueCount()).append(NL);
        output.append("message: ").append(model.message()).append(NL);
        appendAssessment(output, model.retentionAssessment());
        output.append("Before").append(NL);
        output.append(text(model.beforeDiagnostics()));
        output.append("After").append(NL);
        output.append(text(model.afterDiagnostics()));
        return output.toString();
    }

    private static void appendPath(StringBuilder output, String label, String value) {
        String normalized = CliText.trimToEmpty(value);
        if (!normalized.isEmpty()) {
            output.append(label).append(": ").append(normalized).append(NL);
        }
    }

    private static void appendRetention(StringBuilder output, AgentRunStoreRetentionPolicy policy) {
        AgentRunStoreRetentionPolicy model = policy == null
                ? AgentRunStoreRetentionPolicy.defaults()
                : policy;
        output.append("retention: ")
                .append(model.isUnlimited() ? "unlimited" : "bounded")
                .append(" maxRuns=")
                .append(model.maxRuns())
                .append(" maxEventsPerRun=")
                .append(model.maxEventsPerRun())
                .append(NL);
    }

    private static void appendBackupRetention(
            StringBuilder output,
            AgentRunStoreBackupRetentionResult result) {
        if (result == null) {
            return;
        }
        output.append("backupRetention: ")
                .append(result.policy().isUnlimited() ? "unlimited" : "bounded")
                .append(" maxBackups=")
                .append(result.policy().maxBackups())
                .append(" retained=")
                .append(result.retainedBackupCount())
                .append(" pruned=")
                .append(result.prunedBackupCount())
                .append(" failedPrune=")
                .append(result.failedBackupPruneCount())
                .append(NL);
    }

    private static void appendBackupRetentionPolicy(
            StringBuilder output,
            AgentRunStoreBackupRetentionPolicy policy) {
        AgentRunStoreBackupRetentionPolicy model = policy == null
                ? AgentRunStoreBackupRetentionPolicy.defaults()
                : policy;
        output.append("backupRetention: ")
                .append(model.isUnlimited() ? "unlimited" : "bounded")
                .append(" maxBackups=")
                .append(model.maxBackups())
                .append(NL);
    }

    private static void appendBackupInventory(
            StringBuilder output,
            AgentRunStoreBackupInventory inventory) {
        AgentRunStoreBackupInventory model = inventory == null
                ? AgentRunStoreBackupInventory.empty()
                : inventory;
        output.append("backups: ").append(model.backupCount()).append(NL);
        appendPath(output, "latestBackupPath", model.latestBackupPath());
        appendPath(output, "oldestBackupPath", model.oldestBackupPath());
    }

    private static void appendAssessment(
            StringBuilder output,
            AgentRunStoreRetentionAssessment assessment) {
        if (assessment == null) {
            return;
        }
        output.append("retentionPruned: ").append(CliText.yesNo(assessment.pruned())).append(NL);
        output.append("retainedRuns: ").append(assessment.retainedRuns()).append(NL);
        output.append("prunedRuns: ").append(assessment.prunedRuns()).append(NL);
        output.append("retainedEvents: ").append(assessment.retainedEvents()).append(NL);
        output.append("prunedEvents: ").append(assessment.prunedEvents()).append(NL);
    }
}

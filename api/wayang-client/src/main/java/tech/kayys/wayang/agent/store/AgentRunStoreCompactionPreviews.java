package tech.kayys.wayang.agent.store;

/**
 * Factory methods for non-mutating run-store compaction previews.
 */
final class AgentRunStoreCompactionPreviews {

    private AgentRunStoreCompactionPreviews() {
    }

    static AgentRunStoreCompactionPreview fromVerification(
            AgentRunStoreVerification verification,
            boolean dryRun) {
        AgentRunStoreVerification model = verification == null
                ? AgentRunStore.memory().verification()
                : verification;
        AgentRunStoreDiagnostics diagnostics = model.diagnostics();
        return new AgentRunStoreCompactionPreview(
                dryRun,
                diagnostics,
                AgentRunStoreBackupRetentionResult.preview(
                        diagnostics.backupRetentionPolicy(),
                        diagnostics.backupInventory()),
                model.issues());
    }
}

package tech.kayys.wayang.agent.skills.management;

import java.util.Objects;

/**
 * Binds direct artifact synchronization to the managed target artifact store.
 */
final class SkillManagementArtifactSyncWorkflow {

    private final SkillArtifactStore targetArtifacts;
    private final SkillManagementArtifactSyncRunner syncRunner;

    SkillManagementArtifactSyncWorkflow(
            SkillArtifactStore targetArtifacts,
            SkillManagementArtifactSyncRunner syncRunner) {
        this.targetArtifacts = Objects.requireNonNull(targetArtifacts, "targetArtifacts");
        this.syncRunner = Objects.requireNonNull(syncRunner, "syncRunner");
    }

    SkillArtifactStoreSyncResult sync(
            SkillArtifactStore sourceArtifacts,
            SkillArtifactStoreSyncOptions options) {
        Objects.requireNonNull(sourceArtifacts, "sourceArtifacts");
        return syncRunner.sync(sourceArtifacts, targetArtifacts, options);
    }
}

package tech.kayys.wayang.agent.skills.management;

import java.util.Objects;

/**
 * Maps definition and artifact synchronization results to stable admin DTOs.
 */
final class SkillManagementAdminSyncViews {

    private SkillManagementAdminSyncViews() {
    }

    static SkillManagementAdminDefinitionSyncStatus definitionSync(
            SkillDefinitionStoreSyncResult result) {
        Objects.requireNonNull(result, "result");
        return new SkillManagementAdminDefinitionSyncStatus(
                result.dryRun(),
                result.changes().stream()
                        .map(SkillManagementAdminSyncViews::definitionSyncChange)
                        .toList());
    }

    static SkillManagementAdminSyncChange definitionSyncChange(
            SkillDefinitionStoreSyncChange change) {
        Objects.requireNonNull(change, "change");
        return new SkillManagementAdminSyncChange(
                change.skillId(),
                change.action().name(),
                change.changed(),
                change.detail());
    }

    static SkillManagementAdminArtifactSyncStatus artifactSync(
            SkillArtifactStoreSyncResult result) {
        Objects.requireNonNull(result, "result");
        return new SkillManagementAdminArtifactSyncStatus(
                result.dryRun(),
                result.changes().stream()
                        .map(SkillManagementAdminSyncViews::artifactSyncChange)
                        .toList());
    }

    static SkillManagementAdminArtifactSyncChange artifactSyncChange(
            SkillArtifactStoreSyncChange change) {
        Objects.requireNonNull(change, "change");
        return new SkillManagementAdminArtifactSyncChange(
                change.reference().qualifiedName(),
                change.action().name(),
                change.changed(),
                change.detail());
    }
}

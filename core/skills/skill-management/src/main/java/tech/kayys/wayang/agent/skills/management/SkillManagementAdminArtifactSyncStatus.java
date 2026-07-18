package tech.kayys.wayang.agent.skills.management;

import java.util.List;

/**
 * Stable admin-facing projection of artifact-store synchronization.
 */
public record SkillManagementAdminArtifactSyncStatus(
        boolean dryRun,
        long copied,
        long updated,
        long unchanged,
        long conflicts,
        long deleted,
        long changed,
        List<SkillManagementAdminArtifactSyncChange> changes) {

    public SkillManagementAdminArtifactSyncStatus(
            boolean dryRun,
            List<SkillManagementAdminArtifactSyncChange> changes) {
        this(dryRun, 0, 0, 0, 0, 0, 0, changes);
    }

    public SkillManagementAdminArtifactSyncStatus {
        changes = SkillManagementAdminValueSupport.nonNullList(changes);
        SkillManagementAdminSyncSummary summary = SkillManagementAdminSyncSummary.from(changes);
        copied = summary.copied();
        updated = summary.updated();
        unchanged = summary.unchanged();
        conflicts = summary.conflicts();
        deleted = summary.deleted();
        changed = summary.changed();
    }
}

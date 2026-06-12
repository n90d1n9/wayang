package tech.kayys.wayang.agent.skills.management;

import java.util.List;

/**
 * Stable admin-facing projection of definition-store synchronization.
 */
public record SkillManagementAdminDefinitionSyncStatus(
        boolean dryRun,
        long copied,
        long updated,
        long unchanged,
        long conflicts,
        long deleted,
        long changed,
        List<SkillManagementAdminSyncChange> changes) {

    public SkillManagementAdminDefinitionSyncStatus(
            boolean dryRun,
            List<SkillManagementAdminSyncChange> changes) {
        this(dryRun, 0, 0, 0, 0, 0, 0, changes);
    }

    public SkillManagementAdminDefinitionSyncStatus {
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

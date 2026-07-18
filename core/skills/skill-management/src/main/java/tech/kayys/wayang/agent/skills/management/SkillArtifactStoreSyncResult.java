package tech.kayys.wayang.agent.skills.management;

import java.util.List;

/**
 * Summary of a skill artifact store synchronization run.
 */
public record SkillArtifactStoreSyncResult(
        boolean dryRun,
        List<SkillArtifactStoreSyncChange> changes) {

    public SkillArtifactStoreSyncResult {
        changes = SkillManagementValueSupport.nonNullList(changes);
    }

    public long copied() {
        return count(SkillArtifactStoreSyncAction.COPIED);
    }

    public long updated() {
        return count(SkillArtifactStoreSyncAction.UPDATED);
    }

    public long unchanged() {
        return count(SkillArtifactStoreSyncAction.UNCHANGED);
    }

    public long conflicts() {
        return count(SkillArtifactStoreSyncAction.CONFLICT);
    }

    public long deleted() {
        return count(SkillArtifactStoreSyncAction.DELETED);
    }

    public long changed() {
        return SkillManagementValueSupport.countMatching(changes, SkillArtifactStoreSyncChange::changed);
    }

    private long count(SkillArtifactStoreSyncAction action) {
        return SkillManagementValueSupport.countBy(changes, SkillArtifactStoreSyncChange::action, action);
    }
}

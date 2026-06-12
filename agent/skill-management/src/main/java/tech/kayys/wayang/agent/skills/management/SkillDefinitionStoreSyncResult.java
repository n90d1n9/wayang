package tech.kayys.wayang.agent.skills.management;

import java.util.List;

/**
 * Summary of a skill definition store synchronization run.
 */
public record SkillDefinitionStoreSyncResult(
        boolean dryRun,
        List<SkillDefinitionStoreSyncChange> changes) {

    public SkillDefinitionStoreSyncResult {
        changes = SkillManagementValueSupport.nonNullList(changes);
    }

    public long copied() {
        return count(SkillDefinitionStoreSyncAction.COPIED);
    }

    public long updated() {
        return count(SkillDefinitionStoreSyncAction.UPDATED);
    }

    public long unchanged() {
        return count(SkillDefinitionStoreSyncAction.UNCHANGED);
    }

    public long conflicts() {
        return count(SkillDefinitionStoreSyncAction.CONFLICT);
    }

    public long deleted() {
        return count(SkillDefinitionStoreSyncAction.DELETED);
    }

    public long changed() {
        return SkillManagementValueSupport.countMatching(changes, SkillDefinitionStoreSyncChange::changed);
    }

    private long count(SkillDefinitionStoreSyncAction action) {
        return SkillManagementValueSupport.countBy(changes, SkillDefinitionStoreSyncChange::action, action);
    }
}

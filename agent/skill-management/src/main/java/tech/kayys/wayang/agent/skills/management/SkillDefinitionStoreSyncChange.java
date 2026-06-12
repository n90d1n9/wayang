package tech.kayys.wayang.agent.skills.management;

import java.util.Objects;

/**
 * One skill-level change discovered during store synchronization.
 */
public record SkillDefinitionStoreSyncChange(
        String skillId,
        SkillDefinitionStoreSyncAction action,
        String detail) {

    public SkillDefinitionStoreSyncChange {
        if (SkillManagementSkillIds.isBlank(skillId)) {
            throw new IllegalArgumentException("skillId must not be blank");
        }
        action = Objects.requireNonNull(action, "action");
        detail = SkillManagementValueSupport.text(detail);
    }

    public boolean changed() {
        return action == SkillDefinitionStoreSyncAction.COPIED
                || action == SkillDefinitionStoreSyncAction.UPDATED
                || action == SkillDefinitionStoreSyncAction.DELETED;
    }
}

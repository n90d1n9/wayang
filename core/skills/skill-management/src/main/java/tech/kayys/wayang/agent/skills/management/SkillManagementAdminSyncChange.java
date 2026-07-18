package tech.kayys.wayang.agent.skills.management;

/**
 * Stable admin-facing projection of one definition sync change.
 */
public record SkillManagementAdminSyncChange(
        String skillId,
        String action,
        boolean changed,
        String detail) implements SkillManagementAdminSyncChangeView {

    public SkillManagementAdminSyncChange {
        if (SkillManagementSkillIds.isBlank(skillId)) {
            throw new IllegalArgumentException("skillId must not be blank");
        }
        action = SkillManagementAdminValueSupport.action(action);
        changed = SkillManagementAdminValueSupport.changedForSyncAction(action, changed);
        detail = SkillManagementAdminValueSupport.text(detail);
    }
}

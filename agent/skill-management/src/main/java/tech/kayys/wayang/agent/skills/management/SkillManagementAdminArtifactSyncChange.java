package tech.kayys.wayang.agent.skills.management;

/**
 * Stable admin-facing projection of one artifact sync change.
 */
public record SkillManagementAdminArtifactSyncChange(
        String artifactReference,
        String action,
        boolean changed,
        String detail) implements SkillManagementAdminSyncChangeView {

    public SkillManagementAdminArtifactSyncChange {
        artifactReference = SkillManagementAdminValueSupport.text(artifactReference);
        action = SkillManagementAdminValueSupport.action(action);
        changed = SkillManagementAdminValueSupport.changedForSyncAction(action, changed);
        detail = SkillManagementAdminValueSupport.text(detail);
    }
}

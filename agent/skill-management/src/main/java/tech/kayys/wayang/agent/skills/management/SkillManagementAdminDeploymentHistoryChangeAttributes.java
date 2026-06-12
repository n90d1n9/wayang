package tech.kayys.wayang.agent.skills.management;

import static tech.kayys.wayang.agent.skills.management.SkillManagementEventAttributeKeys.*;

/**
 * Deployment history change and sync counts decoded from event attributes.
 */
record SkillManagementAdminDeploymentHistoryChangeAttributes(
        boolean dryRun,
        boolean changed,
        boolean consistent,
        int definitionChanges,
        int artifactChanges,
        int artifactConflicts,
        int lifecycleCreated,
        int lifecycleRemoved) {

    SkillManagementAdminDeploymentHistoryChangeAttributes {
        definitionChanges = SkillManagementAdminValueSupport.nonNegative(definitionChanges);
        artifactChanges = SkillManagementAdminValueSupport.nonNegative(artifactChanges);
        artifactConflicts = SkillManagementAdminValueSupport.nonNegative(artifactConflicts);
        lifecycleCreated = SkillManagementAdminValueSupport.nonNegative(lifecycleCreated);
        lifecycleRemoved = SkillManagementAdminValueSupport.nonNegative(lifecycleRemoved);
    }

    static SkillManagementAdminDeploymentHistoryChangeAttributes empty() {
        return from(null);
    }

    static SkillManagementAdminDeploymentHistoryChangeAttributes from(
            SkillManagementEventAttributeReader attributes) {
        SkillManagementEventAttributeReader reader = SkillManagementEventAttributeReader.orEmpty(attributes);
        return new SkillManagementAdminDeploymentHistoryChangeAttributes(
                reader.flag(DRY_RUN),
                reader.flag(CHANGED),
                reader.flag(CONSISTENT),
                reader.count(DEFINITION_CHANGES),
                reader.count(ARTIFACT_CHANGES),
                reader.count(ARTIFACT_CONFLICTS),
                reader.count(LIFECYCLE_CREATED),
                reader.count(LIFECYCLE_REMOVED));
    }
}

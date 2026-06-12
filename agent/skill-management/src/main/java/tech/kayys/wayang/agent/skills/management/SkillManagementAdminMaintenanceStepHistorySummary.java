package tech.kayys.wayang.agent.skills.management;

/**
 * Stable admin-facing aggregate for one maintenance step across deployment history.
 */
public record SkillManagementAdminMaintenanceStepHistorySummary(
        String step,
        int deployments,
        int dryRunDeployments,
        int skippedDeployments,
        int changedDeployments,
        int consistentDeployments,
        int failedDeployments,
        long changes,
        long conflicts) {

    public SkillManagementAdminMaintenanceStepHistorySummary {
        step = SkillManagementAdminValueSupport.identifier(step);
        deployments = SkillManagementAdminValueSupport.nonNegative(deployments);
        dryRunDeployments = SkillManagementAdminValueSupport.nonNegative(dryRunDeployments);
        skippedDeployments = SkillManagementAdminValueSupport.nonNegative(skippedDeployments);
        changedDeployments = SkillManagementAdminValueSupport.nonNegative(changedDeployments);
        consistentDeployments = SkillManagementAdminValueSupport.nonNegative(consistentDeployments);
        failedDeployments = SkillManagementAdminValueSupport.nonNegative(failedDeployments);
        changes = SkillManagementAdminValueSupport.nonNegative(changes);
        conflicts = SkillManagementAdminValueSupport.nonNegative(conflicts);
    }
}

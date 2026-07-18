package tech.kayys.wayang.agent.skills.management;

import java.util.List;

/**
 * Derived aggregate counts for an admin deployment history page.
 */
record SkillManagementAdminDeploymentHistoryPageSummary(
        int returnedDeployments,
        int successfulDeployments,
        int failedDeployments,
        int changedDeployments,
        int consistentDeployments,
        int preflightDeployments,
        int preflightConfigurationFailures,
        int preflightTargetStoreFailures,
        int preflightSourceStoreFailures,
        int preflightCapabilityFailures,
        List<SkillManagementAdminMaintenanceStepHistorySummary> stepSummaries) {

    SkillManagementAdminDeploymentHistoryPageSummary {
        returnedDeployments = SkillManagementAdminValueSupport.nonNegative(returnedDeployments);
        successfulDeployments = SkillManagementAdminValueSupport.nonNegative(successfulDeployments);
        failedDeployments = SkillManagementAdminValueSupport.nonNegative(failedDeployments);
        changedDeployments = SkillManagementAdminValueSupport.nonNegative(changedDeployments);
        consistentDeployments = SkillManagementAdminValueSupport.nonNegative(consistentDeployments);
        preflightDeployments = SkillManagementAdminValueSupport.nonNegative(preflightDeployments);
        preflightConfigurationFailures = SkillManagementAdminValueSupport.nonNegative(
                preflightConfigurationFailures);
        preflightTargetStoreFailures = SkillManagementAdminValueSupport.nonNegative(preflightTargetStoreFailures);
        preflightSourceStoreFailures = SkillManagementAdminValueSupport.nonNegative(preflightSourceStoreFailures);
        preflightCapabilityFailures = SkillManagementAdminValueSupport.nonNegative(preflightCapabilityFailures);
        stepSummaries = SkillManagementAdminValueSupport.nonNullList(stepSummaries);
    }

    static SkillManagementAdminDeploymentHistoryPageSummary from(
            List<SkillManagementAdminDeploymentHistoryEntry> deployments) {
        List<SkillManagementAdminDeploymentHistoryEntry> entries =
                SkillManagementAdminValueSupport.nonNullList(deployments);
        SkillManagementAdminDeploymentHistoryOutcomeSummary outcome =
                SkillManagementAdminDeploymentHistoryOutcomeSummary.from(entries);
        SkillManagementAdminDeploymentHistoryPreflightSummary preflight =
                SkillManagementAdminDeploymentHistoryPreflightSummary.from(entries);
        return new SkillManagementAdminDeploymentHistoryPageSummary(
                outcome.returnedDeployments(),
                outcome.successfulDeployments(),
                outcome.failedDeployments(),
                outcome.changedDeployments(),
                outcome.consistentDeployments(),
                preflight.preflightDeployments(),
                preflight.configurationFailures(),
                preflight.targetStoreFailures(),
                preflight.sourceStoreFailures(),
                preflight.capabilityFailures(),
                SkillManagementAdminMaintenanceStepHistorySummaries.from(entries));
    }
}

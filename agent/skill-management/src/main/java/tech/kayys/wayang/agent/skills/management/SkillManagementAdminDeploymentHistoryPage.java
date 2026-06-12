package tech.kayys.wayang.agent.skills.management;

import java.util.List;

/**
 * Stable admin-facing projection of recent deployment events.
 */
public record SkillManagementAdminDeploymentHistoryPage(
        int matchedDeployments,
        int returnedDeployments,
        boolean truncated,
        int successfulDeployments,
        int failedDeployments,
        int changedDeployments,
        int consistentDeployments,
        int preflightDeployments,
        int preflightConfigurationFailures,
        int preflightTargetStoreFailures,
        int preflightSourceStoreFailures,
        int preflightCapabilityFailures,
        List<SkillManagementAdminMaintenanceStepHistorySummary> stepSummaries,
        List<SkillManagementAdminDeploymentHistoryEntry> deployments) {

    public SkillManagementAdminDeploymentHistoryPage(
            int matchedDeployments,
            boolean truncated,
            List<SkillManagementAdminDeploymentHistoryEntry> deployments) {
        this(matchedDeployments, 0, truncated, 0, 0, 0, 0, 0, 0, 0, 0, 0, List.of(), deployments);
    }

    public SkillManagementAdminDeploymentHistoryPage(
            int matchedDeployments,
            int returnedDeployments,
            boolean truncated,
            int successfulDeployments,
            int failedDeployments,
            int changedDeployments,
            int consistentDeployments,
            int preflightDeployments,
            int preflightConfigurationFailures,
            int preflightTargetStoreFailures,
            int preflightSourceStoreFailures,
            int preflightCapabilityFailures,
            List<SkillManagementAdminDeploymentHistoryEntry> deployments) {
        this(
                matchedDeployments,
                returnedDeployments,
                truncated,
                successfulDeployments,
                failedDeployments,
                changedDeployments,
                consistentDeployments,
                preflightDeployments,
                preflightConfigurationFailures,
                preflightTargetStoreFailures,
                preflightSourceStoreFailures,
                preflightCapabilityFailures,
                List.of(),
                deployments);
    }

    public SkillManagementAdminDeploymentHistoryPage {
        matchedDeployments = SkillManagementAdminValueSupport.nonNegative(matchedDeployments);
        deployments = SkillManagementAdminValueSupport.nonNullList(deployments);
        SkillManagementAdminDeploymentHistoryPageSummary summary =
                SkillManagementAdminDeploymentHistoryPageSummary.from(deployments);
        returnedDeployments = summary.returnedDeployments();
        matchedDeployments = SkillManagementAdminValueSupport.atLeast(matchedDeployments, returnedDeployments);
        truncated = truncated || matchedDeployments > returnedDeployments;
        successfulDeployments = summary.successfulDeployments();
        failedDeployments = summary.failedDeployments();
        changedDeployments = summary.changedDeployments();
        consistentDeployments = summary.consistentDeployments();
        preflightDeployments = summary.preflightDeployments();
        preflightConfigurationFailures = summary.preflightConfigurationFailures();
        preflightTargetStoreFailures = summary.preflightTargetStoreFailures();
        preflightSourceStoreFailures = summary.preflightSourceStoreFailures();
        preflightCapabilityFailures = summary.preflightCapabilityFailures();
        stepSummaries = summary.stepSummaries();
    }
}

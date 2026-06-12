package tech.kayys.wayang.agent.skills.management;

import java.util.List;
import java.util.function.Predicate;

/**
 * Preflight aggregate counts for an admin deployment history page.
 */
record SkillManagementAdminDeploymentHistoryPreflightSummary(
        int preflightDeployments,
        int configurationFailures,
        int targetStoreFailures,
        int sourceStoreFailures,
        int capabilityFailures) {

    SkillManagementAdminDeploymentHistoryPreflightSummary {
        preflightDeployments = SkillManagementAdminValueSupport.nonNegative(preflightDeployments);
        configurationFailures = SkillManagementAdminValueSupport.nonNegative(configurationFailures);
        targetStoreFailures = SkillManagementAdminValueSupport.nonNegative(targetStoreFailures);
        sourceStoreFailures = SkillManagementAdminValueSupport.nonNegative(sourceStoreFailures);
        capabilityFailures = SkillManagementAdminValueSupport.nonNegative(capabilityFailures);
    }

    static SkillManagementAdminDeploymentHistoryPreflightSummary from(
            List<SkillManagementAdminDeploymentHistoryEntry> deployments) {
        List<SkillManagementAdminDeploymentHistoryEntry> entries =
                SkillManagementAdminValueSupport.nonNullList(deployments);
        return new SkillManagementAdminDeploymentHistoryPreflightSummary(
                count(entries, SkillManagementAdminDeploymentHistoryEntry::preflightAvailable),
                count(entries, entry -> entry.preflightConfigurationErrors() > 0),
                count(entries, entry -> entry.preflightTargetStoreErrors() > 0),
                count(entries, entry -> entry.preflightSourceStoreErrors() > 0),
                count(entries, entry -> entry.preflightCapabilityErrors() > 0));
    }

    private static int count(
            List<SkillManagementAdminDeploymentHistoryEntry> entries,
            Predicate<SkillManagementAdminDeploymentHistoryEntry> predicate) {
        return SkillManagementAdminValueSupport.countMatching(entries, predicate);
    }
}

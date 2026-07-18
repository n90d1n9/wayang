package tech.kayys.wayang.agent.skills.management;

import java.util.List;
import java.util.function.Predicate;

/**
 * Outcome aggregate counts for an admin deployment history page.
 */
record SkillManagementAdminDeploymentHistoryOutcomeSummary(
        int returnedDeployments,
        int successfulDeployments,
        int failedDeployments,
        int changedDeployments,
        int consistentDeployments) {

    SkillManagementAdminDeploymentHistoryOutcomeSummary {
        returnedDeployments = SkillManagementAdminValueSupport.nonNegative(returnedDeployments);
        successfulDeployments = SkillManagementAdminValueSupport.nonNegative(successfulDeployments);
        failedDeployments = SkillManagementAdminValueSupport.nonNegative(failedDeployments);
        changedDeployments = SkillManagementAdminValueSupport.nonNegative(changedDeployments);
        consistentDeployments = SkillManagementAdminValueSupport.nonNegative(consistentDeployments);
    }

    static SkillManagementAdminDeploymentHistoryOutcomeSummary from(
            List<SkillManagementAdminDeploymentHistoryEntry> deployments) {
        List<SkillManagementAdminDeploymentHistoryEntry> entries =
                SkillManagementAdminValueSupport.nonNullList(deployments);
        return new SkillManagementAdminDeploymentHistoryOutcomeSummary(
                entries.size(),
                count(entries, SkillManagementAdminDeploymentHistoryEntry::success),
                count(entries, entry -> !entry.success()),
                count(entries, SkillManagementAdminDeploymentHistoryEntry::changed),
                count(entries, SkillManagementAdminDeploymentHistoryEntry::consistent));
    }

    private static int count(
            List<SkillManagementAdminDeploymentHistoryEntry> entries,
            Predicate<SkillManagementAdminDeploymentHistoryEntry> predicate) {
        return SkillManagementAdminValueSupport.countMatching(entries, predicate);
    }
}

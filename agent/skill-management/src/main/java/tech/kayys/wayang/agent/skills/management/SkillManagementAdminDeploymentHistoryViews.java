package tech.kayys.wayang.agent.skills.management;

import java.util.List;

/**
 * Maps deployment event history to stable admin DTOs.
 */
final class SkillManagementAdminDeploymentHistoryViews {

    private SkillManagementAdminDeploymentHistoryViews() {
    }

    static SkillManagementAdminDeploymentHistoryPage deploymentHistory(
            SkillManagementEventPage page) {
        SkillManagementAdminDeploymentHistoryEventWindow window =
                SkillManagementAdminDeploymentHistoryEventWindow.from(page);
        List<SkillManagementAdminDeploymentHistoryEntry> deployments = window.deploymentEvents().stream()
                .map(SkillManagementAdminDeploymentHistoryViews::deploymentHistoryEntry)
                .toList();
        return new SkillManagementAdminDeploymentHistoryPage(
                window.matchedDeployments(),
                window.truncated(),
                deployments);
    }

    static SkillManagementAdminDeploymentHistoryEntry deploymentHistoryEntry(
            SkillManagementEvent event) {
        return SkillManagementAdminDeploymentHistoryEntry.from(event);
    }
}

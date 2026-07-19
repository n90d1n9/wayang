package tech.kayys.wayang.agent.skills.management;

import java.util.List;

/**
 * Derives maintenance step aggregates from deployment history entries.
 */
final class SkillManagementAdminMaintenanceStepHistorySummaries {

    private SkillManagementAdminMaintenanceStepHistorySummaries() {
    }

    static List<SkillManagementAdminMaintenanceStepHistorySummary> from(
            List<SkillManagementAdminDeploymentHistoryEntry> deployments) {
        return SkillManagementAdminMaintenanceStepHistoryGroups.from(deployments).stream()
                .map(SkillManagementAdminMaintenanceStepHistorySummaries::summary)
                .toList();
    }

    private static SkillManagementAdminMaintenanceStepHistorySummary summary(
            SkillManagementAdminMaintenanceStepHistoryGroup group) {
        return SkillManagementAdminMaintenanceStepHistoryStats.from(group.reports())
                .summary(group.step());
    }
}

package tech.kayys.wayang.agent.skills.management;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Collects maintenance step reports from deployment history entries in stable step order.
 */
final class SkillManagementAdminMaintenanceStepHistoryGroups {

    private SkillManagementAdminMaintenanceStepHistoryGroups() {
    }

    static List<SkillManagementAdminMaintenanceStepHistoryGroup> from(
            List<SkillManagementAdminDeploymentHistoryEntry> deployments) {
        LinkedHashMap<String, List<SkillManagementAdminMaintenanceStepReport>> grouped = orderedGroups();
        SkillManagementAdminValueSupport.nonNullList(deployments).forEach(deployment ->
                SkillManagementAdminValueSupport.nonNullList(deployment.steps()).forEach(step ->
                        grouped.computeIfAbsent(step.step(), ignored -> new ArrayList<>()).add(step)));
        return grouped.entrySet().stream()
                .filter(entry -> !entry.getValue().isEmpty())
                .map(entry -> new SkillManagementAdminMaintenanceStepHistoryGroup(entry.getKey(), entry.getValue()))
                .toList();
    }

    private static LinkedHashMap<String, List<SkillManagementAdminMaintenanceStepReport>> orderedGroups() {
        LinkedHashMap<String, List<SkillManagementAdminMaintenanceStepReport>> grouped = new LinkedHashMap<>();
        for (SkillManagementMaintenanceStep step : SkillManagementMaintenanceStep.values()) {
            grouped.put(step.id(), new ArrayList<>());
        }
        return grouped;
    }
}

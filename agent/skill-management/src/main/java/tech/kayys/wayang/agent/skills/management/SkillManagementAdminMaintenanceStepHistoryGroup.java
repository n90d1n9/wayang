package tech.kayys.wayang.agent.skills.management;

import java.util.List;

/**
 * Grouped maintenance step reports for one deployment-history step id.
 */
record SkillManagementAdminMaintenanceStepHistoryGroup(
        String step,
        List<SkillManagementAdminMaintenanceStepReport> reports) {

    SkillManagementAdminMaintenanceStepHistoryGroup {
        step = SkillManagementAdminValueSupport.identifier(step);
        reports = SkillManagementAdminValueSupport.nonNullList(reports);
    }
}

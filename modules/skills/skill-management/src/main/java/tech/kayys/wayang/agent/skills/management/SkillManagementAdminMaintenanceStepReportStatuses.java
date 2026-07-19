package tech.kayys.wayang.agent.skills.management;

import java.util.Locale;

/**
 * Interprets admin maintenance step report status strings through the core status vocabulary.
 */
final class SkillManagementAdminMaintenanceStepReportStatuses {

    private SkillManagementAdminMaintenanceStepReportStatuses() {
    }

    static boolean failed(SkillManagementAdminMaintenanceStepReport report) {
        return is(report, SkillManagementMaintenanceStepStatus.FAILED);
    }

    static boolean is(
            SkillManagementAdminMaintenanceStepReport report,
            SkillManagementMaintenanceStepStatus expected) {
        return expected != null && expected == status(report);
    }

    static SkillManagementMaintenanceStepStatus status(SkillManagementAdminMaintenanceStepReport report) {
        return report == null ? null : status(report.status());
    }

    static SkillManagementMaintenanceStepStatus status(String value) {
        String normalized = SkillManagementAdminValueSupport.unknownIfBlank(value)
                .trim()
                .toUpperCase(Locale.ROOT);
        for (SkillManagementMaintenanceStepStatus status : SkillManagementMaintenanceStepStatus.values()) {
            if (status.name().equals(normalized)) {
                return status;
            }
        }
        return null;
    }
}

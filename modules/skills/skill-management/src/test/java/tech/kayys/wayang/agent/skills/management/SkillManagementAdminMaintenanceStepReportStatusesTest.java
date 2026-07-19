package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SkillManagementAdminMaintenanceStepReportStatusesTest {

    @Test
    void classifiesFailedReportsThroughCoreStatusVocabulary() {
        assertThat(SkillManagementAdminMaintenanceStepReportStatuses.failed(report("FAILED"))).isTrue();
        assertThat(SkillManagementAdminMaintenanceStepReportStatuses.failed(report("CONFLICT"))).isFalse();
        assertThat(SkillManagementAdminMaintenanceStepReportStatuses.failed(null)).isFalse();
    }

    @Test
    void resolvesLooseStatusTextToKnownStepStatuses() {
        assertThat(SkillManagementAdminMaintenanceStepReportStatuses.status(report(" failed ")))
                .isEqualTo(SkillManagementMaintenanceStepStatus.FAILED);
        assertThat(SkillManagementAdminMaintenanceStepReportStatuses.status(report("dry_run")))
                .isEqualTo(SkillManagementMaintenanceStepStatus.DRY_RUN);
        assertThat(SkillManagementAdminMaintenanceStepReportStatuses.status(report("unknown-status"))).isNull();
        assertThat(SkillManagementAdminMaintenanceStepReportStatuses.status(report(""))).isNull();
    }

    private static SkillManagementAdminMaintenanceStepReport report(String status) {
        return new SkillManagementAdminMaintenanceStepReport(
                "definition-sync",
                status,
                false,
                false,
                false,
                false,
                0,
                0,
                "");
    }
}

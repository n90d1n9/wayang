package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SkillManagementAdminMaintenanceStepHistoryGroupsTest {

    @Test
    void groupsReportsInCanonicalStepOrderThenUnknownStepOrder() {
        SkillManagementAdminDeploymentHistoryEntry first = entry(List.of(
                step("event-prune"),
                step("definition-sync"),
                step("custom-step")));
        SkillManagementAdminDeploymentHistoryEntry second = entry(List.of(
                step("artifact-sync"),
                step("custom-step")));

        List<SkillManagementAdminMaintenanceStepHistoryGroup> groups =
                SkillManagementAdminMaintenanceStepHistoryGroups.from(Arrays.asList(first, null, second));

        assertThat(groups)
                .extracting(SkillManagementAdminMaintenanceStepHistoryGroup::step)
                .containsExactly("definition-sync", "artifact-sync", "event-prune", "custom-step");
        assertThat(groups.get(0).reports()).hasSize(1);
        assertThat(groups.get(1).reports()).hasSize(1);
        assertThat(groups.get(2).reports()).hasSize(1);
        assertThat(groups.get(3).reports()).hasSize(2);
    }

    @Test
    void normalizesGroupFields() {
        SkillManagementAdminMaintenanceStepHistoryGroup group =
                new SkillManagementAdminMaintenanceStepHistoryGroup(" custom-step ", Arrays.asList(null, step("custom-step")));

        assertThat(group.step()).isEqualTo("custom-step");
        assertThat(group.reports()).hasSize(1);
    }

    private static SkillManagementAdminDeploymentHistoryEntry entry(
            List<SkillManagementAdminMaintenanceStepReport> steps) {
        return new SkillManagementAdminDeploymentHistoryEntry(
                "2026-01-01T00:00:00Z",
                "deploy",
                "",
                true,
                false,
                false,
                true,
                0,
                0,
                0,
                0,
                0,
                false,
                false,
                false,
                0,
                false,
                false,
                false,
                0,
                0,
                0,
                0,
                0,
                "",
                "",
                "",
                steps);
    }

    private static SkillManagementAdminMaintenanceStepReport step(String step) {
        return new SkillManagementAdminMaintenanceStepReport(
                step,
                "CHANGED",
                false,
                false,
                true,
                true,
                1,
                0,
                "");
    }
}

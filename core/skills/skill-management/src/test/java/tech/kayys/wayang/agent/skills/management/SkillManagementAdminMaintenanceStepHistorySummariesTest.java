package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SkillManagementAdminMaintenanceStepHistorySummariesTest {

    @Test
    void countsFailedStepsThroughStatusClassifier() {
        List<SkillManagementAdminMaintenanceStepHistorySummary> summaries =
                SkillManagementAdminMaintenanceStepHistorySummaries.from(List.of(entry(List.of(
                        step("definition-sync", " failed "),
                        step("artifact-sync", "CONFLICT")))));

        assertThat(summaries)
                .extracting(SkillManagementAdminMaintenanceStepHistorySummary::step)
                .containsExactly("definition-sync", "artifact-sync");
        assertThat(summaries.get(0).failedDeployments()).isEqualTo(1);
        assertThat(summaries.get(1).failedDeployments()).isZero();
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

    private static SkillManagementAdminMaintenanceStepReport step(String step, String status) {
        return new SkillManagementAdminMaintenanceStepReport(
                step,
                status,
                false,
                false,
                false,
                true,
                0,
                0,
                "");
    }
}

package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class SkillManagementAdminMaintenanceStepHistoryStatsTest {

    @Test
    void derivesStatsFromReports() {
        SkillManagementAdminMaintenanceStepHistoryStats stats =
                SkillManagementAdminMaintenanceStepHistoryStats.from(Arrays.asList(
                        report("FAILED", true, false, false, false, 2, 1),
                        null,
                        report("CHANGED", false, false, true, true, 3, 0),
                        report("SKIPPED", false, true, false, true, 0, 0)));

        assertThat(stats.deployments()).isEqualTo(3);
        assertThat(stats.dryRunDeployments()).isEqualTo(1);
        assertThat(stats.skippedDeployments()).isEqualTo(1);
        assertThat(stats.changedDeployments()).isEqualTo(1);
        assertThat(stats.consistentDeployments()).isEqualTo(2);
        assertThat(stats.failedDeployments()).isEqualTo(1);
        assertThat(stats.changes()).isEqualTo(5);
        assertThat(stats.conflicts()).isEqualTo(1);
    }

    @Test
    void createsAdminSummaryForStep() {
        SkillManagementAdminMaintenanceStepHistorySummary summary =
                new SkillManagementAdminMaintenanceStepHistoryStats(
                        2,
                        1,
                        0,
                        1,
                        1,
                        1,
                        4,
                        2)
                        .summary(" definition-sync ");

        assertThat(summary.step()).isEqualTo("definition-sync");
        assertThat(summary.deployments()).isEqualTo(2);
        assertThat(summary.dryRunDeployments()).isEqualTo(1);
        assertThat(summary.changedDeployments()).isEqualTo(1);
        assertThat(summary.consistentDeployments()).isEqualTo(1);
        assertThat(summary.failedDeployments()).isEqualTo(1);
        assertThat(summary.changes()).isEqualTo(4);
        assertThat(summary.conflicts()).isEqualTo(2);
    }

    @Test
    void normalizesExplicitStats() {
        SkillManagementAdminMaintenanceStepHistoryStats stats =
                new SkillManagementAdminMaintenanceStepHistoryStats(-1, -2, -3, -4, -5, -6, -7, -8);

        assertThat(stats.deployments()).isZero();
        assertThat(stats.dryRunDeployments()).isZero();
        assertThat(stats.skippedDeployments()).isZero();
        assertThat(stats.changedDeployments()).isZero();
        assertThat(stats.consistentDeployments()).isZero();
        assertThat(stats.failedDeployments()).isZero();
        assertThat(stats.changes()).isZero();
        assertThat(stats.conflicts()).isZero();
    }

    private static SkillManagementAdminMaintenanceStepReport report(
            String status,
            boolean dryRun,
            boolean skipped,
            boolean changed,
            boolean consistent,
            long changes,
            long conflicts) {
        return new SkillManagementAdminMaintenanceStepReport(
                "definition-sync",
                status,
                dryRun,
                skipped,
                changed,
                consistent,
                changes,
                conflicts,
                "");
    }
}

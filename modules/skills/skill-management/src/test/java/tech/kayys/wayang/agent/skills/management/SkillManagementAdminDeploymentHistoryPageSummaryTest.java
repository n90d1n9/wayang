package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SkillManagementAdminDeploymentHistoryPageSummaryTest {

    @Test
    void derivesPageCountsFromNormalizedDeploymentEntries() {
        SkillManagementAdminDeploymentHistoryPageSummary summary =
                SkillManagementAdminDeploymentHistoryPageSummary.from(Arrays.asList(
                        entry(
                                true,
                                true,
                                true,
                                false,
                                0,
                                0,
                                0,
                                0,
                                List.of(
                                        step("definition-sync", "CHANGED", false, false, true, true, 2, 0),
                                        step("event-prune", "SKIPPED", false, true, false, true, 0, 0))),
                        null,
                        entry(
                                false,
                                false,
                                false,
                                true,
                                1,
                                0,
                                1,
                                1,
                                List.of(step("definition-sync", "FAILED", false, false, false, false, 0, 1)))));

        assertThat(summary.returnedDeployments()).isEqualTo(2);
        assertThat(summary.successfulDeployments()).isEqualTo(1);
        assertThat(summary.failedDeployments()).isEqualTo(1);
        assertThat(summary.changedDeployments()).isEqualTo(1);
        assertThat(summary.consistentDeployments()).isEqualTo(1);
        assertThat(summary.preflightDeployments()).isEqualTo(1);
        assertThat(summary.preflightConfigurationFailures()).isEqualTo(1);
        assertThat(summary.preflightTargetStoreFailures()).isZero();
        assertThat(summary.preflightSourceStoreFailures()).isEqualTo(1);
        assertThat(summary.preflightCapabilityFailures()).isEqualTo(1);
        assertThat(summary.stepSummaries())
                .extracting(SkillManagementAdminMaintenanceStepHistorySummary::step)
                .containsExactly("definition-sync", "event-prune");
        assertThat(summary.stepSummaries().get(0).deployments()).isEqualTo(2);
        assertThat(summary.stepSummaries().get(0).changedDeployments()).isEqualTo(1);
        assertThat(summary.stepSummaries().get(0).failedDeployments()).isEqualTo(1);
        assertThat(summary.stepSummaries().get(0).changes()).isEqualTo(2);
        assertThat(summary.stepSummaries().get(0).conflicts()).isEqualTo(1);
        assertThat(summary.stepSummaries().get(1).skippedDeployments()).isEqualTo(1);
    }

    @Test
    void normalizesExplicitCountsAndStepSummaries() {
        SkillManagementAdminDeploymentHistoryPageSummary summary =
                new SkillManagementAdminDeploymentHistoryPageSummary(
                        -1,
                        -2,
                        -3,
                        -4,
                        -5,
                        -6,
                        -7,
                        -8,
                        -9,
                        -10,
                        Arrays.asList(null, new SkillManagementAdminMaintenanceStepHistorySummary(
                                "definition-sync",
                                1,
                                0,
                                0,
                                1,
                                1,
                                0,
                                2,
                                0)));

        assertThat(summary.returnedDeployments()).isZero();
        assertThat(summary.successfulDeployments()).isZero();
        assertThat(summary.failedDeployments()).isZero();
        assertThat(summary.changedDeployments()).isZero();
        assertThat(summary.consistentDeployments()).isZero();
        assertThat(summary.preflightDeployments()).isZero();
        assertThat(summary.preflightConfigurationFailures()).isZero();
        assertThat(summary.preflightTargetStoreFailures()).isZero();
        assertThat(summary.preflightSourceStoreFailures()).isZero();
        assertThat(summary.preflightCapabilityFailures()).isZero();
        assertThat(summary.stepSummaries()).hasSize(1);
    }

    private static SkillManagementAdminDeploymentHistoryEntry entry(
            boolean success,
            boolean changed,
            boolean consistent,
            boolean preflightAvailable,
            int configurationFailures,
            int targetStoreFailures,
            int sourceStoreFailures,
            int capabilityFailures,
            List<SkillManagementAdminMaintenanceStepReport> steps) {
        return new SkillManagementAdminDeploymentHistoryEntry(
                "2026-01-01T00:00:00Z",
                "deploy",
                "",
                success,
                false,
                changed,
                consistent,
                0,
                0,
                0,
                0,
                0,
                false,
                false,
                false,
                0,
                preflightAvailable,
                false,
                false,
                configurationFailures + targetStoreFailures + sourceStoreFailures + capabilityFailures,
                configurationFailures,
                targetStoreFailures,
                sourceStoreFailures,
                capabilityFailures,
                "",
                "",
                "",
                steps);
    }

    private static SkillManagementAdminMaintenanceStepReport step(
            String step,
            String status,
            boolean dryRun,
            boolean skipped,
            boolean changed,
            boolean consistent,
            long changes,
            long conflicts) {
        return new SkillManagementAdminMaintenanceStepReport(
                step,
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

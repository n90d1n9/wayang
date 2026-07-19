package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SkillManagementAdminDeploymentHistoryOutcomeSummaryTest {

    @Test
    void derivesOutcomeCountsFromDeploymentEntries() {
        SkillManagementAdminDeploymentHistoryOutcomeSummary summary =
                SkillManagementAdminDeploymentHistoryOutcomeSummary.from(Arrays.asList(
                        entry(true, true, true),
                        null,
                        entry(false, false, false),
                        entry(false, true, false)));

        assertThat(summary.returnedDeployments()).isEqualTo(3);
        assertThat(summary.successfulDeployments()).isEqualTo(1);
        assertThat(summary.failedDeployments()).isEqualTo(2);
        assertThat(summary.changedDeployments()).isEqualTo(2);
        assertThat(summary.consistentDeployments()).isEqualTo(1);
    }

    @Test
    void normalizesExplicitOutcomeCounts() {
        SkillManagementAdminDeploymentHistoryOutcomeSummary summary =
                new SkillManagementAdminDeploymentHistoryOutcomeSummary(-1, -2, -3, -4, -5);

        assertThat(summary.returnedDeployments()).isZero();
        assertThat(summary.successfulDeployments()).isZero();
        assertThat(summary.failedDeployments()).isZero();
        assertThat(summary.changedDeployments()).isZero();
        assertThat(summary.consistentDeployments()).isZero();
    }

    private static SkillManagementAdminDeploymentHistoryEntry entry(
            boolean success,
            boolean changed,
            boolean consistent) {
        return entry(success, changed, consistent, false, 0, 0, 0, 0);
    }

    private static SkillManagementAdminDeploymentHistoryEntry entry(
            boolean success,
            boolean changed,
            boolean consistent,
            boolean preflightAvailable,
            int configurationFailures,
            int targetStoreFailures,
            int sourceStoreFailures,
            int capabilityFailures) {
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
                List.of());
    }
}

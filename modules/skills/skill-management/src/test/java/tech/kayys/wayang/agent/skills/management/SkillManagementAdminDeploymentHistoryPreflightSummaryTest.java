package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SkillManagementAdminDeploymentHistoryPreflightSummaryTest {

    @Test
    void derivesPreflightFailureCountsFromDeploymentEntries() {
        SkillManagementAdminDeploymentHistoryPreflightSummary summary =
                SkillManagementAdminDeploymentHistoryPreflightSummary.from(Arrays.asList(
                        entry(false, 0, 0, 0, 0),
                        null,
                        entry(true, 1, 0, 1, 0),
                        entry(true, 0, 1, 0, 1),
                        entry(true, 1, 1, 1, 1)));

        assertThat(summary.preflightDeployments()).isEqualTo(3);
        assertThat(summary.configurationFailures()).isEqualTo(2);
        assertThat(summary.targetStoreFailures()).isEqualTo(2);
        assertThat(summary.sourceStoreFailures()).isEqualTo(2);
        assertThat(summary.capabilityFailures()).isEqualTo(2);
    }

    @Test
    void normalizesExplicitPreflightCounts() {
        SkillManagementAdminDeploymentHistoryPreflightSummary summary =
                new SkillManagementAdminDeploymentHistoryPreflightSummary(-1, -2, -3, -4, -5);

        assertThat(summary.preflightDeployments()).isZero();
        assertThat(summary.configurationFailures()).isZero();
        assertThat(summary.targetStoreFailures()).isZero();
        assertThat(summary.sourceStoreFailures()).isZero();
        assertThat(summary.capabilityFailures()).isZero();
    }

    private static SkillManagementAdminDeploymentHistoryEntry entry(
            boolean preflightAvailable,
            int configurationFailures,
            int targetStoreFailures,
            int sourceStoreFailures,
            int capabilityFailures) {
        return new SkillManagementAdminDeploymentHistoryEntry(
                "2026-01-01T00:00:00Z",
                "deploy",
                "",
                !preflightAvailable,
                false,
                false,
                false,
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

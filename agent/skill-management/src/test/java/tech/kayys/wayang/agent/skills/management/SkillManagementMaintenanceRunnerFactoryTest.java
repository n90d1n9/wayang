package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SkillManagementMaintenanceRunnerFactoryTest {

    @Test
    void createDerivesPruneCapabilityFromEventSink() {
        SkillManagementEventSink writeOnlySink = event -> {
        };
        SkillManagementMaintenanceRunner runner =
                new SkillManagementMaintenanceRunnerFactory().create(writeOnlySink);

        SkillManagementPreflightReport report = runner.preflight(
                SkillManagementMaintenancePlan.bootstrap()
                        .withEventPruning(SkillManagementEventPruneOptions.keepLatest(1)));

        assertThat(report.ready()).isFalse();
        assertThat(report.capabilityValidation().errors())
                .containsExactly(SkillManagementEventPruner.PRUNE_EVENTS_CAPABILITY_REQUIRED);
    }

    @Test
    void createAcceptsExplicitPrunerForPreResolvedCapability() {
        SkillManagementEventSink writeOnlySink = event -> {
        };
        SkillManagementEventPruner explicitPruner = options ->
                SkillManagementEventPruneResult.skipped(options);
        SkillManagementMaintenanceRunner runner =
                new SkillManagementMaintenanceRunnerFactory().create(writeOnlySink, explicitPruner);

        SkillManagementPreflightReport report = runner.preflight(
                SkillManagementMaintenancePlan.bootstrap()
                        .withEventPruning(SkillManagementEventPruneOptions.keepLatest(1)));

        assertThat(report.ready()).isTrue();
        assertThat(report.capabilityValidation().validConfiguration()).isTrue();
    }
}

package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class SkillManagementMaintenancePreflightTest {

    @Test
    void normalizesNullPlanToBootstrap() {
        assertThat(SkillManagementMaintenancePreflight.plan(null))
                .isEqualTo(SkillManagementMaintenancePlan.bootstrap());
    }

    @Test
    void skipsEventPruneCapabilityValidationWhenPruningIsDisabled() {
        AtomicBoolean called = new AtomicBoolean(false);

        SkillStoreConfigValidationResult result = SkillManagementMaintenancePreflight.validateCapabilities(
                SkillManagementMaintenancePlan.bootstrap(),
                () -> {
                    called.set(true);
                    return SkillStoreConfigValidationResult.error("should not be called");
                });

        assertThat(result.validConfiguration()).isTrue();
        assertThat(called).isFalse();
    }

    @Test
    void reportsUnsupportedEventPrunerInCapabilityBucket() {
        SkillManagementPreflightReport report = SkillManagementMaintenancePreflight.report(
                SkillManagementMaintenancePlan.bootstrap()
                        .withEventPruning(SkillManagementEventPruneOptions.keepLatest(1)),
                SkillManagementEventPruner.unsupported("eventSink"));

        assertThat(report.ready()).isFalse();
        assertThat(report.capabilityValidation().errors())
                .containsExactly(SkillManagementEventPruner.PRUNE_EVENTS_CAPABILITY_REQUIRED);
    }

    @Test
    void treatsNullSupplierResultAsValid() {
        SkillStoreConfigValidationResult result = SkillManagementMaintenancePreflight.validateCapabilities(
                SkillManagementMaintenancePlan.bootstrap()
                        .withEventPruning(SkillManagementEventPruneOptions.keepLatest(1)),
                () -> null);

        assertThat(result.validConfiguration()).isTrue();
    }
}

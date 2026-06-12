package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SkillManagementPreflightEnforcerTest {

    @Test
    void enforcesNeutralMaintenancePreflightAsMaintenanceException() {
        SkillManagementPreflightReport report = new SkillManagementPreflightReport(
                null,
                null,
                null,
                SkillStoreConfigValidationResult.error("missing capability"));

        assertThatThrownBy(() -> SkillManagementPreflightEnforcer.enforce(
                SkillManagementEventOperation.MAINTENANCE,
                report))
                .isInstanceOf(SkillManagementMaintenancePreflightException.class)
                .hasMessageContaining("Skill-management maintenance preflight failed")
                .hasMessageContaining("missing capability");
    }

    @Test
    void recordsDeploymentPreflightFailureWithContext() {
        InMemorySkillManagementEventSink eventSink = new InMemorySkillManagementEventSink();
        SkillManagementOperationContext context = SkillManagementOperationContext.of("deployment-1");
        SkillManagementDeploymentPreflightReport report = new SkillManagementDeploymentPreflightReport(
                SkillManagementDeploymentConfig.defaults(),
                new SkillManagementPreflightReport(
                        null,
                        SkillStoreConfigValidationResult.error("target failed"),
                        null,
                        null));

        assertThatThrownBy(() -> SkillManagementPreflightEnforcer.enforce(
                SkillManagementEventOperation.DEPLOYMENT,
                report,
                eventSink,
                context))
                .isInstanceOf(SkillManagementDeploymentPreflightException.class)
                .hasMessageContaining("target failed");

        assertThat(eventSink.events()).hasSize(1);
        SkillManagementEvent event = eventSink.events().get(0);
        assertThat(event.operation()).isEqualTo(SkillManagementEventOperation.DEPLOYMENT);
        assertThat(event.success()).isFalse();
        assertThat(event.attributes())
                .containsEntry("operationId", "deployment-1")
                .containsEntry("errorType", "SkillManagementDeploymentPreflightException")
                .containsEntry("preflightReady", "false")
                .containsEntry("preflightTargetStoreErrors", "1")
                .containsEntry("preflightTargetStoreMessage", "target failed");
    }

    @Test
    void skipsReadyDeploymentPreflightWithoutRecordingEvents() {
        InMemorySkillManagementEventSink eventSink = new InMemorySkillManagementEventSink();

        SkillManagementPreflightEnforcer.enforce(
                SkillManagementEventOperation.DEPLOYMENT,
                new SkillManagementDeploymentPreflightReport(
                        SkillManagementDeploymentConfig.defaults(),
                        new SkillManagementPreflightReport(null, null, null, null)),
                eventSink,
                SkillManagementOperationContext.of("deployment-1"));

        assertThat(eventSink.events()).isEmpty();
    }
}

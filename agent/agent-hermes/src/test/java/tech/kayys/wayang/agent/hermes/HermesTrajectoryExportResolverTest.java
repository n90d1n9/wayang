package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.spi.AgentRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HermesTrajectoryExportResolverTest {

    @Test
    void resolvesExplicitTrajectoryExportRequest() {
        HermesTrajectoryExportResolver resolver = new HermesTrajectoryExportResolver(HermesAgentModeConfig.builder()
                .trajectoryExportEnabled(true)
                .build());

        HermesTrajectoryExportPlan plan = resolver.resolve(AgentRequest.builder()
                .parameter("exportTrajectory", true)
                .parameter("trajectory.format", "markdown")
                .parameter("trajectory.destination", "s3")
                .parameter("includePrompts", true)
                .parameter("includeToolCalls", false)
                .parameter("redactSensitive", false)
                .build());

        assertThat(plan.exportEnabled()).isTrue();
        assertThat(plan.requested()).isTrue();
        assertThat(plan.export()).isTrue();
        assertThat(plan.active()).isTrue();
        assertThat(plan.format()).isEqualTo("markdown");
        assertThat(plan.destination()).isEqualTo("object-storage");
        assertThat(plan.includePrompts()).isTrue();
        assertThat(plan.includeToolCalls()).isFalse();
        assertThat(plan.redactSensitive()).isFalse();
        assertThat(plan.source()).isEqualTo("explicit");
    }

    @Test
    void infersTrajectoryExportFromPromptWhenFeatureIsEnabled() {
        HermesTrajectoryExportResolver resolver = new HermesTrajectoryExportResolver(HermesAgentModeConfig.builder()
                .trajectoryExportEnabled(true)
                .build());

        HermesTrajectoryExportPlan plan = resolver.resolve(AgentRequest.builder()
                .prompt("Run this and export trace for the audit trail")
                .build());

        assertThat(plan.requested()).isTrue();
        assertThat(plan.export()).isTrue();
        assertThat(plan.format()).isEqualTo("jsonl");
        assertThat(plan.destination()).isEqualTo("local");
        assertThat(plan.includePrompts()).isFalse();
        assertThat(plan.includeToolCalls()).isTrue();
        assertThat(plan.redactSensitive()).isTrue();
        assertThat(plan.source()).isEqualTo("prompt");
        assertThat(plan.reason()).isEqualTo("trajectory export inferred from prompt");
    }

    @Test
    void keepsExportInactiveWhenFeatureIsDisabled() {
        HermesTrajectoryExportResolver resolver = new HermesTrajectoryExportResolver(HermesAgentModeConfig.defaults());

        HermesTrajectoryExportPlan plan = resolver.resolve(AgentRequest.builder()
                .prompt("Export trajectory for this run")
                .build());

        assertThat(plan.exportEnabled()).isFalse();
        assertThat(plan.requested()).isTrue();
        assertThat(plan.export()).isFalse();
        assertThat(plan.active()).isFalse();
        assertThat(plan.destination()).isEqualTo("none");
        assertThat(plan.source()).isEqualTo("disabled");
        assertThat(plan.reason()).isEqualTo("trajectory export disabled");
    }

    @Test
    void respectsExplicitTrajectoryExportOptOut() {
        HermesTrajectoryExportResolver resolver = new HermesTrajectoryExportResolver(HermesAgentModeConfig.builder()
                .trajectoryExportEnabled(true)
                .build());

        HermesTrajectoryExportPlan plan = resolver.resolve(AgentRequest.builder()
                .prompt("Export trace")
                .parameter("trajectory.export", false)
                .build());

        assertThat(plan.exportEnabled()).isTrue();
        assertThat(plan.requested()).isFalse();
        assertThat(plan.export()).isFalse();
        assertThat(plan.active()).isFalse();
        assertThat(plan.source()).isEqualTo("explicit");
        assertThat(plan.reason()).isEqualTo("trajectory export disabled for request");
    }

    @Test
    void disablesExportWhenDestinationIsNone() {
        HermesTrajectoryExportResolver resolver = new HermesTrajectoryExportResolver(HermesAgentModeConfig.builder()
                .trajectoryExportEnabled(true)
                .build());

        HermesTrajectoryExportPlan plan = resolver.resolve(AgentRequest.builder()
                .parameter("trajectory.destination", "none")
                .build());

        assertThat(plan.requested()).isFalse();
        assertThat(plan.export()).isFalse();
        assertThat(plan.destination()).isEqualTo("none");
        assertThat(plan.source()).isEqualTo("explicit");
        assertThat(plan.reason()).isEqualTo("trajectory export destination disabled");
    }

    @Test
    void reportsNoExportForOrdinaryRequests() {
        HermesTrajectoryExportResolver resolver = new HermesTrajectoryExportResolver(HermesAgentModeConfig.builder()
                .trajectoryExportEnabled(true)
                .build());

        HermesTrajectoryExportPlan plan = resolver.resolve(AgentRequest.builder()
                .prompt("Prepare a release report")
                .build());

        assertThat(plan.exportEnabled()).isTrue();
        assertThat(plan.requested()).isFalse();
        assertThat(plan.export()).isFalse();
        assertThat(plan.active()).isFalse();
        assertThat(plan.format()).isEqualTo("jsonl");
        assertThat(plan.destination()).isEqualTo("none");
        assertThat(plan.source()).isEqualTo("none");
    }

    @Test
    void rejectsInvalidTrajectoryBooleans() {
        HermesTrajectoryExportResolver resolver = new HermesTrajectoryExportResolver(HermesAgentModeConfig.builder()
                .trajectoryExportEnabled(true)
                .build());

        assertThatThrownBy(() -> resolver.resolve(AgentRequest.builder()
                .parameter("includePrompts", "maybe")
                .build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("trajectory export boolean");
    }
}

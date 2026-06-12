package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.spi.AgentRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HermesExecutionDirectiveTest {

    @Test
    void activeDirectiveCarriesDispatchPayload() {
        HermesExecutionDirective directive = HermesExecutionDirective.from(
                new HermesExecutionPlan(
                        "docker",
                        "docker",
                        true,
                        true,
                        false,
                        false,
                        List.of("local", "docker", "ssh"),
                        "explicit backend requested"),
                AgentRequest.builder()
                        .requestId("req-a")
                        .tenantId("tenant-a")
                        .sessionId("session-a")
                        .userId("user-a")
                        .build(),
                HermesAgentModeConfig.defaults());

        assertThat(directive.active()).isTrue();
        assertThat(directive.operation()).isEqualTo("dispatch");
        assertThat(directive.backend()).isEqualTo("docker");
        assertThat(directive.adapterType()).isEqualTo("container");
        assertThat(directive.safetyProfile()).isEqualTo("container-isolated");
        assertThat(directive.toMetadata())
                .containsEntry("requestId", "req-a")
                .containsEntry("tenantId", "tenant-a")
                .containsEntry("sessionId", "session-a")
                .containsEntry("userId", "user-a")
                .containsEntry("reason", "explicit backend requested");
    }

    @Test
    void defaultPlanDoesNotDispatchWithoutRequest() {
        HermesExecutionDirective directive = HermesExecutionDirective.from(
                new HermesExecutionPlanner(HermesAgentModeConfig.defaults()).defaultPlan(),
                null,
                HermesAgentModeConfig.defaults());

        assertThat(directive.executable()).isTrue();
        assertThat(directive.backendSupported()).isTrue();
        assertThat(directive.active()).isFalse();
        assertThat(directive.operation()).isEqualTo("none");
        assertThat(directive.backend()).isEqualTo("local");
        assertThat(directive.adapterType()).isEqualTo("local-terminal");
        assertThat(directive.reason()).isEqualTo("default plan only");
    }

    @Test
    void noBackendConfiguredDisablesDispatch() {
        HermesAgentModeConfig config = HermesAgentModeConfig.builder()
                .executionBackends(List.of())
                .build();

        HermesExecutionDirective directive = HermesExecutionDirective.from(
                new HermesExecutionPlanner(config).plan(AgentRequest.builder()
                        .requestId("req-b")
                        .build()),
                AgentRequest.builder()
                        .requestId("req-b")
                        .build(),
                config);

        assertThat(directive.executable()).isFalse();
        assertThat(directive.backendSupported()).isFalse();
        assertThat(directive.active()).isFalse();
        assertThat(directive.operation()).isEqualTo("none");
        assertThat(directive.backend()).isEqualTo("none");
        assertThat(directive.adapterType()).isEqualTo("none");
        assertThat(directive.safetyProfile()).isEqualTo("none");
        assertThat(directive.reason()).isEqualTo("no execution backend configured");
    }
}

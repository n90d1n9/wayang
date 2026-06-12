package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.spi.AgentRequest;

import static org.assertj.core.api.Assertions.assertThat;

class HermesProviderRoutingDirectiveTest {

    @Test
    void activeDirectiveCarriesExplicitProviderRoute() {
        HermesProviderRoutingDirective directive = HermesProviderRoutingDirective.from(
                new HermesProviderRoutingPlan(
                        "openrouter",
                        "openrouter",
                        "auto",
                        "kimi-k2.6",
                        "kimi-k2.6",
                        false,
                        true,
                        true,
                        true,
                        "explicit",
                        "explicit provider requested"),
                AgentRequest.builder()
                        .requestId("req-a")
                        .tenantId("tenant-a")
                        .sessionId("session-a")
                        .userId("user-a")
                        .build());

        assertThat(directive.active()).isTrue();
        assertThat(directive.operation()).isEqualTo("route");
        assertThat(directive.selectedProvider()).isEqualTo("openrouter");
        assertThat(directive.routingMode()).isEqualTo("api-gateway");
        assertThat(directive.model()).isEqualTo("kimi-k2.6");
        assertThat(directive.toMetadata())
                .containsEntry("requestId", "req-a")
                .containsEntry("tenantId", "tenant-a")
                .containsEntry("sessionId", "session-a")
                .containsEntry("userId", "user-a")
                .containsEntry("reason", "explicit provider requested");
    }

    @Test
    void requestWithoutProviderHintsStillRoutesViaRuntimeAutoSelection() {
        HermesProviderRoutingDirective directive = HermesProviderRoutingDirective.from(
                new HermesProviderRoutingResolver(HermesAgentModeConfig.defaults()).resolve(
                        AgentRequest.builder()
                                .requestId("req-b")
                                .prompt("Prepare release report")
                                .build()),
                AgentRequest.builder()
                        .requestId("req-b")
                        .build());

        assertThat(directive.active()).isTrue();
        assertThat(directive.operation()).isEqualTo("route");
        assertThat(directive.selectedProvider()).isEqualTo("auto");
        assertThat(directive.routingMode()).isEqualTo("auto");
        assertThat(directive.toolCallingRequired()).isTrue();
        assertThat(directive.reason()).isEqualTo("provider selection delegated to runtime");
    }

    @Test
    void defaultPlanDoesNotRouteWithoutRequestOrRoutingSignal() {
        HermesProviderRoutingDirective directive = HermesProviderRoutingDirective.from(
                new HermesProviderRoutingResolver(HermesAgentModeConfig.defaults()).defaultPlan(),
                null);

        assertThat(directive.active()).isFalse();
        assertThat(directive.operation()).isEqualTo("none");
        assertThat(directive.selectedProvider()).isEqualTo("auto");
        assertThat(directive.routingMode()).isEqualTo("auto");
        assertThat(directive.reason()).isEqualTo("default plan only");
    }
}

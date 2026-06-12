package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.spi.AgentRequest;

import static org.assertj.core.api.Assertions.assertThat;

class HermesGatewayDirectiveTest {

    @Test
    void activeGatewayDirectiveCarriesDeliveryPayload() {
        HermesGatewayContext context = new HermesGatewayContext(
                "telegram",
                "chat-42",
                "topic-7",
                "",
                "msg-9",
                "tenant-a",
                "session-a",
                "user-a",
                "fulan",
                "corr-a",
                true);

        HermesGatewayDirective directive = HermesGatewayDirective.from(
                context,
                AgentRequest.builder()
                        .requestId("req-a")
                        .tenantId("tenant-a")
                        .sessionId("session-a")
                        .userId("user-a")
                        .build(),
                HermesAgentModeConfig.defaults());

        assertThat(directive.active()).isTrue();
        assertThat(directive.operation()).isEqualTo("deliver");
        assertThat(directive.platform()).isEqualTo("telegram");
        assertThat(directive.destinationType()).isEqualTo("thread");
        assertThat(directive.destinationId()).isEqualTo("topic-7");
        assertThat(directive.continuityKey()).isEqualTo("telegram:chat-42:topic-7");
        assertThat(directive.toMetadata())
                .containsEntry("channelId", "chat-42")
                .containsEntry("messageId", "msg-9")
                .containsEntry("tenantId", "tenant-a")
                .containsEntry("userId", "user-a")
                .containsEntry("reason", "gateway delivery requested");
    }

    @Test
    void disabledGatewayKeepsDestinationButDisablesOperation() {
        HermesGatewayContext context = new HermesGatewayContext(
                "slack",
                "ops",
                "",
                "",
                "",
                "tenant-a",
                "",
                "",
                "",
                "",
                false);

        HermesGatewayDirective directive = HermesGatewayDirective.from(
                context,
                AgentRequest.builder().requestId("req-b").build(),
                HermesAgentModeConfig.builder()
                        .gatewayEnabled(false)
                        .build());

        assertThat(directive.gatewayEnabled()).isFalse();
        assertThat(directive.supportedPlatform()).isFalse();
        assertThat(directive.active()).isFalse();
        assertThat(directive.operation()).isEqualTo("none");
        assertThat(directive.destinationType()).isEqualTo("channel");
        assertThat(directive.destinationId()).isEqualTo("ops");
        assertThat(directive.reason()).isEqualTo("gateway disabled");
    }

    @Test
    void defaultPlanIsNotActiveWithoutRequest() {
        HermesGatewayDirective directive = HermesGatewayDirective.from(
                null,
                null,
                HermesAgentModeConfig.defaults());

        assertThat(directive.gatewayEnabled()).isTrue();
        assertThat(directive.supportedPlatform()).isTrue();
        assertThat(directive.active()).isFalse();
        assertThat(directive.operation()).isEqualTo("none");
        assertThat(directive.destinationType()).isEqualTo("local");
        assertThat(directive.reason()).isEqualTo("default plan only");
    }
}

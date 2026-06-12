package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.spi.AgentRequest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HermesGatewayContextResolverTest {

    @Test
    void resolvesGatewayAliasesIntoStableContext() {
        HermesGatewayContextResolver resolver = new HermesGatewayContextResolver(HermesAgentModeConfig.defaults());
        AgentRequest request = AgentRequest.builder()
                .requestId("req-gateway")
                .tenantId("tenant-a")
                .sessionId("session-a")
                .userId("user-a")
                .context(Map.of(
                        "gateway.platform", "telegram",
                        "chatId", "chat-42",
                        "messageId", "msg-7"))
                .parameter("threadId", "topic-2")
                .parameter("username", "fulan")
                .build();

        HermesGatewayContext context = resolver.resolve(request);

        assertThat(context.platform()).isEqualTo("telegram");
        assertThat(context.channelId()).isEqualTo("chat-42");
        assertThat(context.threadId()).isEqualTo("topic-2");
        assertThat(context.conversationId()).isEqualTo("session-a");
        assertThat(context.messageId()).isEqualTo("msg-7");
        assertThat(context.userId()).isEqualTo("user-a");
        assertThat(context.username()).isEqualTo("fulan");
        assertThat(context.supportedPlatform()).isTrue();
        assertThat(context.continuityKey()).isEqualTo("telegram:session-a");
    }

    @Test
    void infersPlatformFromChannelWhenItNamesSupportedGateway() {
        HermesGatewayContextResolver resolver = new HermesGatewayContextResolver(HermesAgentModeConfig.defaults());

        HermesGatewayContext context = resolver.resolve(AgentRequest.builder()
                .context(Map.of("channel", "slack"))
                .build());

        assertThat(context.platform()).isEqualTo("slack");
        assertThat(context.channelId()).isEmpty();
        assertThat(context.supportedPlatform()).isTrue();
    }

    @Test
    void treatsUnknownChannelAsChannelIdWithCliFallback() {
        HermesGatewayContextResolver resolver = new HermesGatewayContextResolver(HermesAgentModeConfig.defaults());

        HermesGatewayContext context = resolver.resolve(AgentRequest.builder()
                .context(Map.of("channel", "release-room"))
                .build());

        assertThat(context.platform()).isEqualTo("cli");
        assertThat(context.channelId()).isEqualTo("release-room");
        assertThat(context.continuityKey()).isEqualTo("cli:release-room");
    }

    @Test
    void marksUnsupportedPlatformsWhenGatewayIsDisabled() {
        HermesGatewayContextResolver resolver = new HermesGatewayContextResolver(HermesAgentModeConfig.builder()
                .gatewayEnabled(false)
                .gatewayPlatforms(List.of("cli", "telegram"))
                .build());

        HermesGatewayContext context = resolver.resolve(AgentRequest.builder()
                .context(Map.of("platform", "telegram"))
                .build());

        assertThat(context.platform()).isEqualTo("telegram");
        assertThat(context.supportedPlatform()).isFalse();
        assertThat(context.toMetadata())
                .containsEntry("platform", "telegram")
                .containsEntry("supportedPlatform", false);
    }
}

package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aJsonRpcCoreMethodHandlerContributionsTest {

    @Test
    void exposesStableCoreContributorMetadata() {
        assertThat(WayangA2aJsonRpcCoreMethodHandlerContributions.sendMessage().toMap())
                .containsEntry("providerId", WayangA2aJsonRpcCoreMethodHandlerContributions.PROVIDER_SEND_MESSAGE)
                .containsEntry("moduleId", WayangA2aJsonRpcCoreMethodHandlerContributions.MODULE_ID)
                .containsEntry("priority", 0);
        assertThat(WayangA2aMaps.stringList(
                WayangA2aJsonRpcCoreMethodHandlerContributions.sendMessage()
                        .toMap()
                        .get("capabilityTags")))
                .containsExactly(
                        WayangA2aJsonRpcCoreMethodHandlerContributions.TAG_A2A_JSON_RPC,
                        WayangA2aJsonRpcCoreMethodHandlerContributions.GROUP_SEND_MESSAGE,
                        "streaming");
        assertThat(WayangA2aJsonRpcCoreMethodHandlerContributions.task().providerId())
                .isEqualTo(WayangA2aJsonRpcCoreMethodHandlerContributions.PROVIDER_TASK);
        assertThat(WayangA2aJsonRpcCoreMethodHandlerContributions.agentCard().providerId())
                .isEqualTo(WayangA2aJsonRpcCoreMethodHandlerContributions.PROVIDER_AGENT_CARD);
    }
}

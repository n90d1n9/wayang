package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2a.core.A2aProtocol;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aSpecAlignmentJsonRpcRequirementsTest {

    @Test
    void exposesPinnedJsonRpcRequirementsInStableOrder() {
        List<WayangA2aSpecAlignmentRequirement> requirements =
                WayangA2aSpecAlignmentJsonRpcRequirements.requirements();

        assertThat(requirements).hasSize(3);
        assertThat(requirements.stream().map(WayangA2aSpecAlignmentRequirement::id))
                .containsExactly(
                        "jsonrpc.method_registry",
                        "jsonrpc.response_media",
                        "jsonrpc.capability_gates");
        assertThat(requirements)
                .allSatisfy(requirement -> assertThat(requirement.category()).isEqualTo("jsonrpc"))
                .allSatisfy(requirement -> assertThat(requirement.aligned()).isTrue());
    }

    @Test
    void pinsMethodRegistryAgainstCanonicalRouteCatalog() {
        WayangA2aSpecAlignmentRequirement registry =
                WayangA2aSpecAlignmentJsonRpcRequirements.registryRequirement();

        assertThat(map(registry.actual()))
                .containsEntry("methodCount", WayangA2aJsonRpcMethods.methods().size())
                .containsEntry("methods", WayangA2aJsonRpcMethods.methods());
        assertThat(map(registry.expected()))
                .containsEntry("methodCount", 11)
                .containsEntry("methods", WayangA2aJsonRpcMethods.methods());
    }

    @Test
    void pinsResponseMediaAndCapabilityGates() {
        WayangA2aSpecAlignmentRequirement responseMedia =
                WayangA2aSpecAlignmentJsonRpcRequirements.responseMediaRequirement();
        WayangA2aSpecAlignmentRequirement capabilityGates =
                WayangA2aSpecAlignmentJsonRpcRequirements.capabilityGateRequirement();

        assertThat(strings(map(responseMedia.actual()).get("streamingMethods")))
                .containsExactly(
                        WayangA2aJsonRpcMethods.SEND_STREAMING_MESSAGE,
                        WayangA2aJsonRpcMethods.SUBSCRIBE_TO_TASK);
        assertThat(map(responseMedia.actual()))
                .containsEntry("jsonMediaType", WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON)
                .containsEntry("streamingMediaType", A2aProtocol.EVENT_STREAM_MEDIA_TYPE);
        assertThat(strings(map(capabilityGates.actual()).get("streamingMethods")))
                .containsExactly(
                        WayangA2aJsonRpcMethods.SEND_STREAMING_MESSAGE,
                        WayangA2aJsonRpcMethods.SUBSCRIBE_TO_TASK);
        assertThat(strings(map(capabilityGates.actual()).get("pushNotificationMethods")))
                .containsExactly(
                        WayangA2aJsonRpcMethods.CREATE_TASK_PUSH_NOTIFICATION_CONFIG,
                        WayangA2aJsonRpcMethods.GET_TASK_PUSH_NOTIFICATION_CONFIG,
                        WayangA2aJsonRpcMethods.LIST_TASK_PUSH_NOTIFICATION_CONFIGS,
                        WayangA2aJsonRpcMethods.DELETE_TASK_PUSH_NOTIFICATION_CONFIG);
        assertThat(strings(map(capabilityGates.actual()).get("extendedAgentCardMethods")))
                .containsExactly(WayangA2aJsonRpcMethods.GET_EXTENDED_AGENT_CARD);
    }

    private static Map<String, Object> map(Object value) {
        assertThat(value).isInstanceOf(Map.class);
        return WayangA2aMaps.copyMap((Map<?, ?>) value);
    }

    @SuppressWarnings("unchecked")
    private static List<String> strings(Object value) {
        assertThat(value).isInstanceOf(List.class);
        return (List<String>) value;
    }
}

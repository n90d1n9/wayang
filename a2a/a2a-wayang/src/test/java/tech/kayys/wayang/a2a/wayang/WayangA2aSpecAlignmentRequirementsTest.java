package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2a.core.A2aHttpRoute;
import tech.kayys.wayang.a2a.core.A2aHttpRouteCatalog;
import tech.kayys.wayang.a2a.core.A2aProtocol;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aSpecAlignmentRequirementsTest {

    @Test
    void assemblesPinnedRequirementsInStableOrder() {
        List<WayangA2aSpecAlignmentRequirement> requirements =
                WayangA2aSpecAlignmentRequirements.from(A2aHttpRouteCatalog.standard());

        assertThat(requirements).hasSize(20);
        assertThat(requirements.stream().map(WayangA2aSpecAlignmentRequirement::id))
                .containsExactly(
                        "protocol.metadata",
                        "binding.metadata",
                        "agent_card.top_level_fields",
                        "agent_card.component_fields",
                        "agent_card.binding_defaults",
                        "route." + A2aProtocol.OPERATION_DISCOVER_AGENT_CARD,
                        "route." + A2aProtocol.OPERATION_SEND_MESSAGE,
                        "route." + A2aProtocol.OPERATION_SEND_STREAMING_MESSAGE,
                        "route." + A2aProtocol.OPERATION_GET_TASK,
                        "route." + A2aProtocol.OPERATION_LIST_TASKS,
                        "route." + A2aProtocol.OPERATION_CANCEL_TASK,
                        "route." + A2aProtocol.OPERATION_SUBSCRIBE_TO_TASK,
                        "route." + A2aProtocol.OPERATION_CREATE_TASK_PUSH_NOTIFICATION_CONFIG,
                        "route." + A2aProtocol.OPERATION_GET_TASK_PUSH_NOTIFICATION_CONFIG,
                        "route." + A2aProtocol.OPERATION_LIST_TASK_PUSH_NOTIFICATION_CONFIGS,
                        "route." + A2aProtocol.OPERATION_DELETE_TASK_PUSH_NOTIFICATION_CONFIG,
                        "route." + A2aProtocol.OPERATION_GET_EXTENDED_AGENT_CARD,
                        "jsonrpc.method_registry",
                        "jsonrpc.response_media",
                        "jsonrpc.capability_gates");
        assertThat(requirements).allSatisfy(requirement -> assertThat(requirement.aligned()).isTrue());
    }

    @Test
    void reportsMissingRoutesAsGaps() {
        A2aHttpRoute discover = A2aHttpRouteCatalog.standard()
                .routeForOperation(A2aProtocol.OPERATION_DISCOVER_AGENT_CARD)
                .orElseThrow();

        List<WayangA2aSpecAlignmentRequirement> requirements =
                WayangA2aSpecAlignmentRequirements.from(new A2aHttpRouteCatalog(List.of(discover)));

        assertThat(requirements.stream()
                        .filter(requirement -> !requirement.aligned())
                        .map(WayangA2aSpecAlignmentRequirement::id))
                .contains("route." + A2aProtocol.OPERATION_SEND_MESSAGE);
        WayangA2aSpecAlignmentRequirement sendMessage = requirements.stream()
                .filter(requirement -> requirement.id().equals("route." + A2aProtocol.OPERATION_SEND_MESSAGE))
                .findFirst()
                .orElseThrow();
        assertThat(sendMessage.category()).isEqualTo("route");
        assertThat(sendMessage.message()).contains("missing");
        assertThat(sendMessage.actual()).containsEntry("present", false);
    }
}

package tech.kayys.wayang.a2a.wayang;

import tech.kayys.wayang.a2a.core.A2aHttpRoute;
import tech.kayys.wayang.a2a.core.A2aHttpRouteCatalog;
import tech.kayys.wayang.a2a.core.A2aProtocol;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

final class WayangA2aSpecAlignmentJsonRpcRequirements {

    private static final List<String> JSON_RPC_METHODS = A2aHttpRouteCatalog.standard().routes().stream()
            .map(A2aHttpRoute::jsonRpcMethod)
            .filter(Objects::nonNull)
            .toList();
    private static final List<String> STREAMING_METHODS = List.of(
            A2aProtocol.OPERATION_SEND_STREAMING_MESSAGE,
            A2aProtocol.OPERATION_SUBSCRIBE_TO_TASK);
    private static final List<String> PUSH_NOTIFICATION_METHODS = List.of(
            A2aProtocol.OPERATION_CREATE_TASK_PUSH_NOTIFICATION_CONFIG,
            A2aProtocol.OPERATION_GET_TASK_PUSH_NOTIFICATION_CONFIG,
            A2aProtocol.OPERATION_LIST_TASK_PUSH_NOTIFICATION_CONFIGS,
            A2aProtocol.OPERATION_DELETE_TASK_PUSH_NOTIFICATION_CONFIG);
    private static final List<String> EXTENDED_AGENT_CARD_METHODS = List.of(
            A2aProtocol.OPERATION_GET_EXTENDED_AGENT_CARD);

    private WayangA2aSpecAlignmentJsonRpcRequirements() {
    }

    static List<WayangA2aSpecAlignmentRequirement> requirements() {
        return List.of(
                registryRequirement(),
                responseMediaRequirement(),
                capabilityGateRequirement());
    }

    static WayangA2aSpecAlignmentRequirement registryRequirement() {
        Map<String, Object> expected = Map.of(
                "methodCount", JSON_RPC_METHODS.size(),
                "methods", JSON_RPC_METHODS);
        Map<String, Object> actual = Map.of(
                "methodCount", WayangA2aJsonRpcMethods.methods().size(),
                "methods", WayangA2aJsonRpcMethods.methods());
        return WayangA2aSpecAlignmentRequirementFactory.compare(
                "jsonrpc.method_registry",
                "jsonrpc",
                "A2A JSON-RPC method registry",
                expected,
                actual,
                "A2A JSON-RPC method registry does not match the pinned route catalog snapshot.");
    }

    static WayangA2aSpecAlignmentRequirement responseMediaRequirement() {
        Map<String, Object> expected = Map.of(
                "jsonMethods", JSON_RPC_METHODS.stream()
                        .filter(method -> !STREAMING_METHODS.contains(method))
                        .toList(),
                "streamingMethods", STREAMING_METHODS,
                "jsonMediaType", WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON,
                "streamingMediaType", A2aProtocol.EVENT_STREAM_MEDIA_TYPE);
        Map<String, Object> actual = Map.of(
                "jsonMethods", WayangA2aJsonRpcMethods.methods().stream()
                        .filter(method -> WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON
                                .equals(WayangA2aJsonRpcMethods.responseMediaType(method)))
                        .toList(),
                "streamingMethods", WayangA2aJsonRpcMethods.streamingMethods(),
                "jsonMediaType", WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON,
                "streamingMediaType", A2aProtocol.EVENT_STREAM_MEDIA_TYPE);
        return WayangA2aSpecAlignmentRequirementFactory.compare(
                "jsonrpc.response_media",
                "jsonrpc",
                "A2A JSON-RPC response media types",
                expected,
                actual,
                "A2A JSON-RPC response media types do not match the pinned route catalog snapshot.");
    }

    static WayangA2aSpecAlignmentRequirement capabilityGateRequirement() {
        Map<String, Object> expected = Map.of(
                "streamingMethods", STREAMING_METHODS,
                "pushNotificationMethods", PUSH_NOTIFICATION_METHODS,
                "extendedAgentCardMethods", EXTENDED_AGENT_CARD_METHODS);
        Map<String, Object> actual = Map.of(
                "streamingMethods", methodsWhere(WayangA2aJsonRpcMethods::streaming),
                "pushNotificationMethods", methodsWhere(WayangA2aJsonRpcMethods::requiresPushNotificationCapability),
                "extendedAgentCardMethods", methodsWhere(WayangA2aJsonRpcMethods::requiresExtendedAgentCardCapability));
        return WayangA2aSpecAlignmentRequirementFactory.compare(
                "jsonrpc.capability_gates",
                "jsonrpc",
                "A2A JSON-RPC capability gates",
                expected,
                actual,
                "A2A JSON-RPC capability gates do not match the pinned route catalog snapshot.");
    }

    private static List<String> methodsWhere(Predicate<String> predicate) {
        return WayangA2aJsonRpcMethods.methods().stream()
                .filter(predicate)
                .toList();
    }
}

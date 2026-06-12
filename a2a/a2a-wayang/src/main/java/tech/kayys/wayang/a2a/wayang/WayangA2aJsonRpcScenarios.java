package tech.kayys.wayang.a2a.wayang;

import tech.kayys.wayang.a2a.core.A2aMessage;
import tech.kayys.wayang.a2a.core.A2aSendMessageRequest;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Built-in A2A JSON-RPC smoke scenario definitions.
 */
public final class WayangA2aJsonRpcScenarios {

    private WayangA2aJsonRpcScenarios() {
    }

    public static WayangA2aJsonRpcScenario smoke(A2aSendMessageRequest sendMessageRequest) {
        String taskId = WayangA2aSendMessageIdentity.requiredMessageTaskId(
                sendMessageRequest,
                "Smoke scenario requires a request message taskId");
        A2aSendMessageRequest streamingRequest = streamingRequest(sendMessageRequest, taskId + "-stream");
        String streamingTaskId = WayangA2aSendMessageIdentity.requiredMessageTaskId(
                streamingRequest,
                "Smoke scenario requires a request message taskId");
        return new WayangA2aJsonRpcScenario(
                "a2a.jsonrpc.smoke",
                "A2A JSON-RPC send, task read, list, stream, push config, and extended card smoke scenario",
                List.of(
                        exchange("send", WayangA2aJsonRpcMethods.SEND_MESSAGE, sendMessageRequest.toMap()),
                        exchange("get", WayangA2aJsonRpcMethods.GET_TASK, taskParams(taskId)),
                        exchange("list", WayangA2aJsonRpcMethods.LIST_TASKS, listParams(sendMessageRequest, taskId)),
                        exchange("stream", WayangA2aJsonRpcMethods.SEND_STREAMING_MESSAGE,
                                streamingRequest.toMap()),
                        exchange("push-create", WayangA2aJsonRpcMethods.CREATE_TASK_PUSH_NOTIFICATION_CONFIG,
                                pushCreateParams(taskId)),
                        exchange("push-get", WayangA2aJsonRpcMethods.GET_TASK_PUSH_NOTIFICATION_CONFIG,
                                pushConfigParams(taskId)),
                        exchange("push-delete", WayangA2aJsonRpcMethods.DELETE_TASK_PUSH_NOTIFICATION_CONFIG,
                                pushConfigParams(taskId)),
                        exchange("card", WayangA2aJsonRpcMethods.GET_EXTENDED_AGENT_CARD, Map.of())),
                smokeAttributes(taskId, streamingTaskId, sendMessageRequest));
    }

    public static WayangA2aJsonRpcScenario methodError(String id, String method) {
        return new WayangA2aJsonRpcScenario(
                id,
                "A2A JSON-RPC method error scenario",
                List.of(WayangA2aJsonRpcScenarioExchange.of(
                        WayangA2aJsonRpcRequest.of("error", method, Map.of()))),
                Map.of());
    }

    private static WayangA2aJsonRpcScenarioExchange exchange(
            Object id,
            String method,
            Map<String, Object> params) {
        return WayangA2aJsonRpcScenarioExchange.of(WayangA2aJsonRpcRequest.of(id, method, params));
    }

    private static Map<String, Object> listParams(A2aSendMessageRequest request, String taskId) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("contextId", WayangA2aSendMessageIdentity.contextId(request, taskId));
        params.put("pageSize", 50);
        return WayangA2aMaps.copyMap(params);
    }

    private static Map<String, Object> taskParams(String taskId) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("id", taskId);
        return WayangA2aMaps.copyMap(params);
    }

    private static Map<String, Object> pushCreateParams(String taskId) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("taskId", taskId);
        params.put("configId", "smoke");
        params.put("url", "https://hooks.example/a2a");
        return WayangA2aMaps.copyMap(params);
    }

    private static Map<String, Object> pushConfigParams(String taskId) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("taskId", taskId);
        params.put("id", "smoke");
        return WayangA2aMaps.copyMap(params);
    }

    private static Map<String, Object> smokeAttributes(
            String taskId,
            String streamingTaskId,
            A2aSendMessageRequest sendMessageRequest) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("taskId", taskId);
        attributes.put("streamingTaskId", streamingTaskId);
        attributes.put("messageId", sendMessageRequest.message().messageId());
        return WayangA2aMaps.copyMap(attributes);
    }

    private static A2aSendMessageRequest streamingRequest(A2aSendMessageRequest request, String taskId) {
        A2aMessage message = request.message();
        return new A2aSendMessageRequest(
                request.tenant(),
                new A2aMessage(
                        message.messageId() + "-stream",
                        message.contextId(),
                        taskId,
                        message.role(),
                        message.parts(),
                        message.metadata(),
                        message.extensions(),
                        message.referenceTaskIds()),
                request.configuration(),
                request.metadata());
    }
}

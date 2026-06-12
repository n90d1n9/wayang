package tech.kayys.wayang.a2a.core;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Canonical HTTP route catalog for current A2A protocol operations.
 */
public record A2aHttpRouteCatalog(List<A2aHttpRoute> routes) {

    public A2aHttpRouteCatalog {
        routes = A2aValues.copyRecords(routes);
    }

    public static A2aHttpRouteCatalog standard() {
        return new A2aHttpRouteCatalog(List.of(
                route(A2aProtocol.OPERATION_DISCOVER_AGENT_CARD, null, null, "GET",
                        A2aProtocol.WELL_KNOWN_AGENT_CARD_PATH, false, "Discover public Agent Card"),
                route(A2aProtocol.OPERATION_SEND_MESSAGE, A2aProtocol.OPERATION_SEND_MESSAGE,
                        A2aProtocol.OPERATION_SEND_MESSAGE, "POST", "/message:send", false, "Send message"),
                route(A2aProtocol.OPERATION_SEND_STREAMING_MESSAGE, A2aProtocol.OPERATION_SEND_STREAMING_MESSAGE,
                        A2aProtocol.OPERATION_SEND_STREAMING_MESSAGE, "POST", "/message:stream", true,
                        "Send streaming message"),
                route(A2aProtocol.OPERATION_GET_TASK, A2aProtocol.OPERATION_GET_TASK,
                        A2aProtocol.OPERATION_GET_TASK, "GET", "/tasks/{id}", false, "Get task"),
                route(A2aProtocol.OPERATION_LIST_TASKS, A2aProtocol.OPERATION_LIST_TASKS,
                        A2aProtocol.OPERATION_LIST_TASKS, "GET", "/tasks", false, "List tasks"),
                route(A2aProtocol.OPERATION_CANCEL_TASK, A2aProtocol.OPERATION_CANCEL_TASK,
                        A2aProtocol.OPERATION_CANCEL_TASK, "POST", "/tasks/{id}:cancel", false, "Cancel task"),
                route(A2aProtocol.OPERATION_SUBSCRIBE_TO_TASK, A2aProtocol.OPERATION_SUBSCRIBE_TO_TASK,
                        A2aProtocol.OPERATION_SUBSCRIBE_TO_TASK, "POST", "/tasks/{id}:subscribe", true,
                        "Subscribe to task updates"),
                route(A2aProtocol.OPERATION_CREATE_TASK_PUSH_NOTIFICATION_CONFIG,
                        A2aProtocol.OPERATION_CREATE_TASK_PUSH_NOTIFICATION_CONFIG,
                        A2aProtocol.OPERATION_CREATE_TASK_PUSH_NOTIFICATION_CONFIG, "POST",
                        "/tasks/{id}/pushNotificationConfigs", false, "Create push notification config"),
                route(A2aProtocol.OPERATION_GET_TASK_PUSH_NOTIFICATION_CONFIG,
                        A2aProtocol.OPERATION_GET_TASK_PUSH_NOTIFICATION_CONFIG,
                        A2aProtocol.OPERATION_GET_TASK_PUSH_NOTIFICATION_CONFIG, "GET",
                        "/tasks/{id}/pushNotificationConfigs/{configId}", false, "Get push notification config"),
                route(A2aProtocol.OPERATION_LIST_TASK_PUSH_NOTIFICATION_CONFIGS,
                        A2aProtocol.OPERATION_LIST_TASK_PUSH_NOTIFICATION_CONFIGS,
                        A2aProtocol.OPERATION_LIST_TASK_PUSH_NOTIFICATION_CONFIGS, "GET",
                        "/tasks/{id}/pushNotificationConfigs", false, "List push notification configs"),
                route(A2aProtocol.OPERATION_DELETE_TASK_PUSH_NOTIFICATION_CONFIG,
                        A2aProtocol.OPERATION_DELETE_TASK_PUSH_NOTIFICATION_CONFIG,
                        A2aProtocol.OPERATION_DELETE_TASK_PUSH_NOTIFICATION_CONFIG, "DELETE",
                        "/tasks/{id}/pushNotificationConfigs/{configId}", false, "Delete push notification config"),
                route(A2aProtocol.OPERATION_GET_EXTENDED_AGENT_CARD,
                        A2aProtocol.OPERATION_GET_EXTENDED_AGENT_CARD,
                        A2aProtocol.OPERATION_GET_EXTENDED_AGENT_CARD, "GET",
                        "/extendedAgentCard", false, "Get authenticated extended Agent Card")));
    }

    public Optional<A2aHttpRoute> routeForOperation(String operation) {
        String normalized = A2aValues.required(operation, "operation");
        return routes.stream()
                .filter(route -> route.operation().equals(normalized))
                .findFirst();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("protocolVersion", A2aProtocol.VERSION);
        payload.put("routeCount", routes.size());
        payload.put("routes", routes.stream().map(A2aHttpRoute::toMap).toList());
        return A2aValues.copyMap(payload);
    }

    private static A2aHttpRoute route(
            String operation,
            String jsonRpcMethod,
            String grpcMethod,
            String httpMethod,
            String path,
            boolean streaming,
            String description) {
        return new A2aHttpRoute(operation, jsonRpcMethod, grpcMethod, httpMethod, path, streaming, description);
    }
}

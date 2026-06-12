package tech.kayys.wayang.a2a.wayang;

import tech.kayys.wayang.a2a.core.A2aHttpRoute;
import tech.kayys.wayang.a2a.core.A2aProtocol;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * JSON-RPC method registry for the A2A v1.0 binding.
 */
public final class WayangA2aJsonRpcMethods {

    public static final String SEND_MESSAGE = A2aProtocol.OPERATION_SEND_MESSAGE;
    public static final String SEND_STREAMING_MESSAGE = A2aProtocol.OPERATION_SEND_STREAMING_MESSAGE;
    public static final String GET_TASK = A2aProtocol.OPERATION_GET_TASK;
    public static final String LIST_TASKS = A2aProtocol.OPERATION_LIST_TASKS;
    public static final String CANCEL_TASK = A2aProtocol.OPERATION_CANCEL_TASK;
    public static final String SUBSCRIBE_TO_TASK = A2aProtocol.OPERATION_SUBSCRIBE_TO_TASK;
    public static final String CREATE_TASK_PUSH_NOTIFICATION_CONFIG =
            A2aProtocol.OPERATION_CREATE_TASK_PUSH_NOTIFICATION_CONFIG;
    public static final String GET_TASK_PUSH_NOTIFICATION_CONFIG =
            A2aProtocol.OPERATION_GET_TASK_PUSH_NOTIFICATION_CONFIG;
    public static final String LIST_TASK_PUSH_NOTIFICATION_CONFIGS =
            A2aProtocol.OPERATION_LIST_TASK_PUSH_NOTIFICATION_CONFIGS;
    public static final String DELETE_TASK_PUSH_NOTIFICATION_CONFIG =
            A2aProtocol.OPERATION_DELETE_TASK_PUSH_NOTIFICATION_CONFIG;
    public static final String GET_EXTENDED_AGENT_CARD = A2aProtocol.OPERATION_GET_EXTENDED_AGENT_CARD;

    public static final String METHOD_GROUP_SEND = "send";
    public static final String METHOD_GROUP_TASK_QUERY = "taskQuery";
    public static final String METHOD_GROUP_TASK_LIFECYCLE = "taskLifecycle";
    public static final String METHOD_GROUP_TASK_SUBSCRIPTION = "taskSubscription";
    public static final String METHOD_GROUP_PUSH_CONFIG = "pushConfig";
    public static final String METHOD_GROUP_AGENT_CARD = "agentCard";

    private WayangA2aJsonRpcMethods() {
    }

    public static Optional<Descriptor> descriptor(String method) {
        return WayangA2aJsonRpcMethodCatalog.descriptor(method);
    }

    public static Optional<Descriptor> operationDescriptor(String operation) {
        return WayangA2aJsonRpcMethodCatalog.operationDescriptor(operation);
    }

    public static Descriptor requireDescriptor(String method) {
        return descriptor(method)
                .orElseThrow(() -> new IllegalArgumentException("Unsupported JSON-RPC method: " + method));
    }

    public static Optional<String> operation(String method) {
        return descriptor(method).map(Descriptor::operation);
    }

    public static List<String> methods() {
        return WayangA2aJsonRpcMethodCatalog.methods();
    }

    public static Map<String, List<String>> methodGroups() {
        return WayangA2aJsonRpcMethodCatalog.methodGroups();
    }

    public static Optional<String> methodGroup(String method) {
        return WayangA2aJsonRpcMethodCatalog.methodGroup(method);
    }

    public static List<Descriptor> descriptors() {
        return WayangA2aJsonRpcMethodCatalog.descriptors();
    }

    public static List<String> streamingMethods() {
        return WayangA2aJsonRpcMethodCatalog.streamingMethods();
    }

    public static boolean streaming(String methodOrOperation) {
        return descriptorForMethodOrOperation(methodOrOperation)
                .map(Descriptor::requiresStreamingCapability)
                .orElse(false);
    }

    public static boolean requiresPushNotificationCapability(String methodOrOperation) {
        return descriptorForMethodOrOperation(methodOrOperation)
                .map(Descriptor::requiresPushNotificationCapability)
                .orElse(false);
    }

    public static boolean requiresExtendedAgentCardCapability(String methodOrOperation) {
        return descriptorForMethodOrOperation(methodOrOperation)
                .map(Descriptor::requiresExtendedAgentCardCapability)
                .orElse(false);
    }

    public static String responseMediaType(String method) {
        return descriptor(method)
                .map(Descriptor::responseMediaType)
                .orElse(WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON);
    }

    public static WayangA2aSpecAlignmentReport specAlignmentReport() {
        return WayangA2aSpecAlignmentReport.defaults();
    }

    private static Optional<Descriptor> descriptorForMethodOrOperation(String methodOrOperation) {
        return WayangA2aJsonRpcMethodCatalog.descriptorForMethodOrOperation(methodOrOperation);
    }

    public record Descriptor(
            String method,
            String operation,
            String grpcMethod,
            String restMethod,
            String restPath,
            boolean streaming,
            String requestMediaType,
            String responseMediaType,
            String description) {

        public Descriptor {
            method = WayangA2aMaps.required(method, "method");
            operation = WayangA2aMaps.required(operation, "operation");
            grpcMethod = grpcMethod == null ? "" : grpcMethod.trim();
            restMethod = WayangA2aHttpRequest.normalizeMethod(restMethod);
            restPath = WayangA2aHttpRequest.normalizePath(restPath);
            requestMediaType = requestMediaType == null || requestMediaType.isBlank()
                    ? WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON
                    : requestMediaType.trim();
            responseMediaType = responseMediaType == null || responseMediaType.isBlank()
                    ? defaultResponseMediaType(streaming)
                    : responseMediaType.trim();
            description = description == null ? "" : description.trim();
        }

        static Descriptor fromRoute(A2aHttpRoute route) {
            A2aHttpRoute resolved = Objects.requireNonNull(route, "route");
            return new Descriptor(
                    resolved.jsonRpcMethod(),
                    resolved.operation(),
                    resolved.grpcMethod(),
                    resolved.httpMethod(),
                    resolved.path(),
                    resolved.streaming(),
                    WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON,
                    defaultResponseMediaType(resolved.streaming()),
                    resolved.description());
        }

        boolean requiresStreamingCapability() {
            return streaming;
        }

        boolean requiresPushNotificationCapability() {
            return A2aProtocol.OPERATION_CREATE_TASK_PUSH_NOTIFICATION_CONFIG.equals(operation)
                    || A2aProtocol.OPERATION_GET_TASK_PUSH_NOTIFICATION_CONFIG.equals(operation)
                    || A2aProtocol.OPERATION_LIST_TASK_PUSH_NOTIFICATION_CONFIGS.equals(operation)
                    || A2aProtocol.OPERATION_DELETE_TASK_PUSH_NOTIFICATION_CONFIG.equals(operation);
        }

        boolean requiresExtendedAgentCardCapability() {
            return A2aProtocol.OPERATION_GET_EXTENDED_AGENT_CARD.equals(operation);
        }

        Map<String, Object> toBindingReportMap(String endpointPath) {
            Map<String, Object> values = new LinkedHashMap<>();
            values.put("method", method);
            values.put("operation", operation);
            values.put("endpointPath", WayangA2aHttpRequest.normalizePath(endpointPath));
            values.put("httpMethod", "POST");
            values.put("requestMediaType", requestMediaType);
            values.put("responseMediaType", responseMediaType);
            values.put("streaming", streaming);
            return WayangA2aMaps.copyMap(values);
        }

        private static String defaultResponseMediaType(boolean streaming) {
            return streaming
                    ? A2aProtocol.EVENT_STREAM_MEDIA_TYPE
                    : WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON;
        }
    }
}

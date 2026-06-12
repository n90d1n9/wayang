package tech.kayys.wayang.a2a.wayang;

import tech.kayys.wayang.a2a.core.A2aHttpRouteCatalog;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class WayangA2aJsonRpcMethodCatalog {

    private static final List<WayangA2aJsonRpcMethods.Descriptor> DESCRIPTORS = descriptorsFromCatalog();
    private static final Map<String, WayangA2aJsonRpcMethods.Descriptor> DESCRIPTORS_BY_METHOD =
            descriptorsByMethod(DESCRIPTORS);
    private static final Map<String, WayangA2aJsonRpcMethods.Descriptor> DESCRIPTORS_BY_OPERATION =
            descriptorsByOperation(DESCRIPTORS);
    private static final List<String> METHODS = DESCRIPTORS.stream()
            .map(WayangA2aJsonRpcMethods.Descriptor::method)
            .toList();
    private static final Map<String, List<String>> METHOD_GROUPS = createMethodGroups();
    private static final Map<String, String> METHOD_GROUP_BY_METHOD = methodGroupByMethod(METHOD_GROUPS);

    private WayangA2aJsonRpcMethodCatalog() {
    }

    static Optional<WayangA2aJsonRpcMethods.Descriptor> descriptor(String method) {
        return Optional.ofNullable(DESCRIPTORS_BY_METHOD.get(WayangA2aMaps.optional(method)));
    }

    static Optional<WayangA2aJsonRpcMethods.Descriptor> operationDescriptor(String operation) {
        return Optional.ofNullable(DESCRIPTORS_BY_OPERATION.get(WayangA2aMaps.optional(operation)));
    }

    static Optional<WayangA2aJsonRpcMethods.Descriptor> descriptorForMethodOrOperation(String methodOrOperation) {
        String normalized = WayangA2aMaps.optional(methodOrOperation);
        if (normalized == null) {
            return Optional.empty();
        }
        WayangA2aJsonRpcMethods.Descriptor method = DESCRIPTORS_BY_METHOD.get(normalized);
        return method == null ? Optional.ofNullable(DESCRIPTORS_BY_OPERATION.get(normalized)) : Optional.of(method);
    }

    static List<String> methods() {
        return METHODS;
    }

    static Map<String, List<String>> methodGroups() {
        return METHOD_GROUPS;
    }

    static Optional<String> methodGroup(String method) {
        return Optional.ofNullable(METHOD_GROUP_BY_METHOD.get(WayangA2aMaps.optional(method)));
    }

    static List<WayangA2aJsonRpcMethods.Descriptor> descriptors() {
        return DESCRIPTORS;
    }

    static List<String> streamingMethods() {
        return DESCRIPTORS.stream()
                .filter(WayangA2aJsonRpcMethods.Descriptor::streaming)
                .map(WayangA2aJsonRpcMethods.Descriptor::method)
                .toList();
    }

    private static List<WayangA2aJsonRpcMethods.Descriptor> descriptorsFromCatalog() {
        return A2aHttpRouteCatalog.standard().routes().stream()
                .filter(route -> WayangA2aMaps.optional(route.jsonRpcMethod()) != null)
                .map(WayangA2aJsonRpcMethods.Descriptor::fromRoute)
                .toList();
    }

    private static Map<String, WayangA2aJsonRpcMethods.Descriptor> descriptorsByMethod(
            List<WayangA2aJsonRpcMethods.Descriptor> descriptors) {
        Map<String, WayangA2aJsonRpcMethods.Descriptor> values = new LinkedHashMap<>();
        for (WayangA2aJsonRpcMethods.Descriptor descriptor : descriptors) {
            WayangA2aJsonRpcMethods.Descriptor previous = values.putIfAbsent(descriptor.method(), descriptor);
            if (previous != null) {
                throw new IllegalStateException("Duplicate A2A JSON-RPC method: " + descriptor.method());
            }
        }
        return Collections.unmodifiableMap(values);
    }

    private static Map<String, WayangA2aJsonRpcMethods.Descriptor> descriptorsByOperation(
            List<WayangA2aJsonRpcMethods.Descriptor> descriptors) {
        Map<String, WayangA2aJsonRpcMethods.Descriptor> values = new LinkedHashMap<>();
        for (WayangA2aJsonRpcMethods.Descriptor descriptor : descriptors) {
            WayangA2aJsonRpcMethods.Descriptor previous = values.putIfAbsent(descriptor.operation(), descriptor);
            if (previous != null) {
                throw new IllegalStateException("Duplicate A2A operation for JSON-RPC method: "
                        + descriptor.operation());
            }
        }
        return Collections.unmodifiableMap(values);
    }

    private static Map<String, List<String>> createMethodGroups() {
        Map<String, List<String>> values = new LinkedHashMap<>();
        putMethodGroup(values,
                WayangA2aJsonRpcMethods.METHOD_GROUP_SEND,
                WayangA2aJsonRpcMethods.SEND_MESSAGE,
                WayangA2aJsonRpcMethods.SEND_STREAMING_MESSAGE);
        putMethodGroup(values,
                WayangA2aJsonRpcMethods.METHOD_GROUP_TASK_QUERY,
                WayangA2aJsonRpcMethods.GET_TASK,
                WayangA2aJsonRpcMethods.LIST_TASKS);
        putMethodGroup(values,
                WayangA2aJsonRpcMethods.METHOD_GROUP_TASK_LIFECYCLE,
                WayangA2aJsonRpcMethods.CANCEL_TASK);
        putMethodGroup(values,
                WayangA2aJsonRpcMethods.METHOD_GROUP_TASK_SUBSCRIPTION,
                WayangA2aJsonRpcMethods.SUBSCRIBE_TO_TASK);
        putMethodGroup(values,
                WayangA2aJsonRpcMethods.METHOD_GROUP_PUSH_CONFIG,
                WayangA2aJsonRpcMethods.CREATE_TASK_PUSH_NOTIFICATION_CONFIG,
                WayangA2aJsonRpcMethods.GET_TASK_PUSH_NOTIFICATION_CONFIG,
                WayangA2aJsonRpcMethods.LIST_TASK_PUSH_NOTIFICATION_CONFIGS,
                WayangA2aJsonRpcMethods.DELETE_TASK_PUSH_NOTIFICATION_CONFIG);
        putMethodGroup(values,
                WayangA2aJsonRpcMethods.METHOD_GROUP_AGENT_CARD,
                WayangA2aJsonRpcMethods.GET_EXTENDED_AGENT_CARD);
        return Collections.unmodifiableMap(values);
    }

    private static void putMethodGroup(Map<String, List<String>> values, String group, String... methods) {
        List<String> registeredMethods = List.of(methods).stream()
                .filter(METHODS::contains)
                .toList();
        if (!registeredMethods.isEmpty()) {
            values.put(group, registeredMethods);
        }
    }

    private static Map<String, String> methodGroupByMethod(Map<String, List<String>> methodGroups) {
        Map<String, String> values = new LinkedHashMap<>();
        methodGroups.forEach((group, methods) -> methods.forEach(method -> {
            String previous = values.putIfAbsent(method, group);
            if (previous != null) {
                throw new IllegalStateException("Duplicate A2A JSON-RPC method group for method: " + method);
            }
        }));
        return Collections.unmodifiableMap(values);
    }
}

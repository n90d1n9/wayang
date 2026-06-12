package tech.kayys.wayang.a2a.wayang;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Runtime dispatch table for A2A JSON-RPC methods.
 */
final class WayangA2aJsonRpcMethodDispatchTable {

    private final Map<String, Entry> entries;

    private WayangA2aJsonRpcMethodDispatchTable(Map<String, Entry> entries) {
        this.entries = Collections.unmodifiableMap(new LinkedHashMap<>(entries));
    }

    static WayangA2aJsonRpcMethodDispatchTable of(Map<String, Handler> handlers) {
        Map<String, Handler> normalized = normalizeHandlers(handlers);
        Map<String, Entry> entries = new LinkedHashMap<>();
        for (WayangA2aJsonRpcMethods.Descriptor descriptor : WayangA2aJsonRpcMethods.descriptors()) {
            Handler handler = normalized.get(descriptor.method());
            if (handler != null) {
                entries.put(descriptor.method(), new Entry(descriptor, methodGroup(descriptor.method()), handler));
            }
        }
        return new WayangA2aJsonRpcMethodDispatchTable(entries);
    }

    static WayangA2aJsonRpcMethodDispatchTable requireComplete(Map<String, Handler> handlers) {
        WayangA2aJsonRpcMethodDispatchTable table = of(handlers);
        if (!table.complete()) {
            throw new IllegalStateException("Missing A2A JSON-RPC dispatch handlers: "
                    + String.join(", ", table.missingRegisteredMethods()));
        }
        return table;
    }

    Optional<Entry> entry(String method) {
        return Optional.ofNullable(entries.get(WayangA2aMaps.optional(method)));
    }

    boolean supports(String method) {
        return entry(method).isPresent();
    }

    boolean complete() {
        return missingRegisteredMethods().isEmpty();
    }

    int methodCount() {
        return entries.size();
    }

    List<String> methods() {
        return List.copyOf(entries.keySet());
    }

    Map<String, List<String>> methodGroups() {
        Map<String, List<String>> values = new LinkedHashMap<>();
        for (String group : WayangA2aJsonRpcMethods.methodGroups().keySet()) {
            List<String> methods = entries.values().stream()
                    .filter(entry -> group.equals(entry.methodGroup()))
                    .map(Entry::method)
                    .toList();
            if (!methods.isEmpty()) {
                values.put(group, methods);
            }
        }
        entries.values().stream()
                .map(Entry::methodGroup)
                .filter(group -> !values.containsKey(group))
                .forEach(group -> {
                    List<String> methods = methodsInGroup(group);
                    if (!methods.isEmpty()) {
                        values.put(group, methods);
                    }
                });
        return Collections.unmodifiableMap(values);
    }

    List<String> missingRegisteredMethods() {
        return WayangA2aJsonRpcMethods.methods().stream()
                .filter(method -> !entries.containsKey(method))
                .toList();
    }

    WayangA2aJsonRpcMethodDispatchCoverage coverage() {
        return WayangA2aJsonRpcMethodDispatchCoverage.from(this);
    }

    private static Map<String, Handler> normalizeHandlers(Map<String, Handler> handlers) {
        if (handlers == null || handlers.isEmpty()) {
            return Map.of();
        }
        Map<String, Handler> values = new LinkedHashMap<>();
        handlers.forEach((method, handler) -> {
            String normalized = WayangA2aMaps.required(method, "method");
            WayangA2aJsonRpcMethods.requireDescriptor(normalized);
            values.put(normalized, Objects.requireNonNull(handler, "handler"));
        });
        return values;
    }

    private static String methodGroup(String method) {
        return WayangA2aJsonRpcMethods.methodGroup(method).orElse("unassigned");
    }

    private List<String> methodsInGroup(String group) {
        return entries.values().stream()
                .filter(entry -> group.equals(entry.methodGroup()))
                .map(Entry::method)
                .toList();
    }

    @FunctionalInterface
    interface Handler {

        WayangA2aHttpResponse dispatch(
                WayangA2aJsonRpcRequest request,
                WayangA2aSendMessagePreflight.JsonRpcResult preflight);
    }

    record Entry(WayangA2aJsonRpcMethods.Descriptor descriptor, String methodGroup, Handler handler) {

        Entry {
            descriptor = Objects.requireNonNull(descriptor, "descriptor");
            methodGroup = WayangA2aMaps.required(methodGroup, "methodGroup");
            handler = Objects.requireNonNull(handler, "handler");
        }

        String method() {
            return descriptor.method();
        }

        String operation() {
            return descriptor.operation();
        }

        boolean streaming() {
            return descriptor.streaming();
        }

        WayangA2aHttpResponse dispatch(
                WayangA2aJsonRpcRequest request,
                WayangA2aSendMessagePreflight.JsonRpcResult preflight) {
            return handler.dispatch(request, preflight);
        }
    }
}

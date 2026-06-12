package tech.kayys.wayang.a2a.wayang;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable assembly result for contributed JSON-RPC method handler groups.
 */
record WayangA2aJsonRpcMethodHandlerRegistryAssembly(
        Map<String, WayangA2aJsonRpcMethodDispatchTable.Handler> handlers,
        List<WayangA2aJsonRpcMethodHandlerOverride> overrides) {

    WayangA2aJsonRpcMethodHandlerRegistryAssembly {
        handlers = handlers == null || handlers.isEmpty()
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(handlers));
        overrides = overrides == null || overrides.isEmpty()
                ? List.of()
                : List.copyOf(overrides);
    }

    static WayangA2aJsonRpcMethodHandlerRegistryAssembly from(
            List<WayangA2aJsonRpcMethodHandlerGroup> groups,
            WayangA2aJsonRpcMethodHandlerOverridePolicy overridePolicy) {
        List<WayangA2aJsonRpcMethodHandlerGroup> resolvedGroups =
                groups == null ? List.of() : List.copyOf(groups);
        WayangA2aJsonRpcMethodHandlerRegistryValidation.requireUniqueGroupNames(resolvedGroups);
        WayangA2aJsonRpcMethodHandlerOverridePolicy policy =
                Objects.requireNonNull(overridePolicy, "overridePolicy");

        Map<String, WayangA2aJsonRpcMethodDispatchTable.Handler> values = new LinkedHashMap<>();
        Map<String, String> owningGroups = new LinkedHashMap<>();
        List<WayangA2aJsonRpcMethodHandlerOverride> overrides = new ArrayList<>();
        for (WayangA2aJsonRpcMethodHandlerGroup group : resolvedGroups) {
            group.handlers().forEach((method, handler) -> {
                String previousGroup = owningGroups.get(method);
                if (previousGroup != null) {
                    WayangA2aJsonRpcMethodHandlerOverride override =
                            new WayangA2aJsonRpcMethodHandlerOverride(method, previousGroup, group.name());
                    policy.validate(override);
                    overrides.add(override);
                }
                values.put(method, handler);
                owningGroups.put(method, group.name());
            });
        }
        return new WayangA2aJsonRpcMethodHandlerRegistryAssembly(values, overrides);
    }

    List<Map<String, Object>> overrideMaps() {
        return overrides.stream()
                .map(WayangA2aJsonRpcMethodHandlerOverride::toMap)
                .toList();
    }
}

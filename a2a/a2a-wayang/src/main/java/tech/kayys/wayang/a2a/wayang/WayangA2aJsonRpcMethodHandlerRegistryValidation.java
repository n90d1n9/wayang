package tech.kayys.wayang.a2a.wayang;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Validation guardrails for JSON-RPC method handler registry contributions.
 */
final class WayangA2aJsonRpcMethodHandlerRegistryValidation {

    private WayangA2aJsonRpcMethodHandlerRegistryValidation() {
    }

    static void requireGroup(
            String groupName,
            Map<String, WayangA2aJsonRpcMethodDispatchTable.Handler> handlers) {
        String resolvedGroup = WayangA2aMaps.required(groupName, "groupName");
        if (handlers == null || handlers.isEmpty()) {
            throw new IllegalArgumentException("A2A JSON-RPC method handler group "
                    + resolvedGroup
                    + " must contribute at least one handler.");
        }
        for (String method : handlers.keySet()) {
            try {
                WayangA2aJsonRpcMethods.requireDescriptor(method);
            } catch (IllegalArgumentException error) {
                throw new IllegalArgumentException("A2A JSON-RPC method handler group "
                        + resolvedGroup
                        + " contributed unsupported method: "
                        + method, error);
            }
        }
    }

    static void requireUniqueGroupNames(List<WayangA2aJsonRpcMethodHandlerGroup> groups) {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        for (WayangA2aJsonRpcMethodHandlerGroup group : groups == null
                ? List.<WayangA2aJsonRpcMethodHandlerGroup>of()
                : groups) {
            String name = group.name();
            if (!names.add(name)) {
                throw new IllegalArgumentException("Duplicate A2A JSON-RPC method handler group: " + name);
            }
        }
    }
}

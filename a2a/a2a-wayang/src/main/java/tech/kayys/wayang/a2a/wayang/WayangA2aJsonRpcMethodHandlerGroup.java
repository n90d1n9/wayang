package tech.kayys.wayang.a2a.wayang;

import java.util.Map;

/**
 * Named group of JSON-RPC method handlers contributed to the dispatch registry.
 */
record WayangA2aJsonRpcMethodHandlerGroup(
        String name,
        Map<String, WayangA2aJsonRpcMethodDispatchTable.Handler> handlers,
        WayangA2aJsonRpcMethodHandlerContribution contribution) {

    WayangA2aJsonRpcMethodHandlerGroup {
        name = WayangA2aMaps.required(name, "name");
        handlers = WayangA2aJsonRpcMethodHandlerMaps.builder()
                .putAll(handlers)
                .build();
        WayangA2aJsonRpcMethodHandlerRegistryValidation.requireGroup(name, handlers);
        contribution = contribution == null
                ? WayangA2aJsonRpcMethodHandlerContribution.forGroup(name)
                : contribution;
    }

    static WayangA2aJsonRpcMethodHandlerGroup of(
            String name,
            Map<String, WayangA2aJsonRpcMethodDispatchTable.Handler> handlers) {
        return new WayangA2aJsonRpcMethodHandlerGroup(name, handlers, null);
    }

    static WayangA2aJsonRpcMethodHandlerGroup of(
            String name,
            Map<String, WayangA2aJsonRpcMethodDispatchTable.Handler> handlers,
            WayangA2aJsonRpcMethodHandlerContribution contribution) {
        return new WayangA2aJsonRpcMethodHandlerGroup(name, handlers, contribution);
    }
}

package tech.kayys.wayang.a2a.wayang;

import java.util.Map;

/**
 * One JSON-RPC request in an A2A harness scenario.
 */
public record WayangA2aJsonRpcScenarioExchange(
        WayangA2aJsonRpcRequest request,
        Map<String, Object> attributes) {

    public WayangA2aJsonRpcScenarioExchange {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        attributes = WayangA2aMaps.copyMap(attributes);
    }

    public static WayangA2aJsonRpcScenarioExchange of(WayangA2aJsonRpcRequest request) {
        return new WayangA2aJsonRpcScenarioExchange(request, Map.of());
    }

    public Map<String, Object> toMap() {
        return WayangA2aScenarioExchangeDefinitionProjection.exchange(
                request.id(),
                request.method(),
                null,
                null,
                null,
                request.params(),
                attributes);
    }
}

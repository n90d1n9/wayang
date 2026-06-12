package tech.kayys.wayang.a2a.wayang;

import java.util.List;
import java.util.Map;

/**
 * Ordered A2A JSON-RPC smoke or contract scenario.
 */
public record WayangA2aJsonRpcScenario(
        String id,
        String description,
        List<WayangA2aJsonRpcScenarioExchange> exchanges,
        Map<String, Object> attributes) {

    public WayangA2aJsonRpcScenario {
        id = WayangA2aMaps.required(id, "id");
        description = WayangA2aMaps.optional(description);
        exchanges = exchanges == null || exchanges.isEmpty()
                ? List.of()
                : exchanges.stream().toList();
        if (exchanges.isEmpty()) {
            throw new IllegalArgumentException("A2A JSON-RPC scenario requires at least one exchange");
        }
        attributes = WayangA2aMaps.copyMap(attributes);
    }

    public static WayangA2aJsonRpcScenario of(String id, List<WayangA2aJsonRpcRequest> requests) {
        return new WayangA2aJsonRpcScenario(
                id,
                null,
                requests.stream().map(WayangA2aJsonRpcScenarioExchange::of).toList(),
                Map.of());
    }

    public Map<String, Object> toMap() {
        return WayangA2aScenarioDefinitionProjection.definition(
                id,
                description,
                exchanges,
                WayangA2aJsonRpcScenarioExchange::toMap,
                attributes);
    }
}

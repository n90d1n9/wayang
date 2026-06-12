package tech.kayys.wayang.a2a.wayang;

import java.util.List;
import java.util.Map;

/**
 * Ordered A2A HTTP smoke or contract scenario.
 */
public record WayangA2aHttpScenario(
        String id,
        String description,
        List<WayangA2aHttpScenarioExchange> exchanges,
        Map<String, Object> attributes) {

    public WayangA2aHttpScenario {
        id = WayangA2aMaps.required(id, "id");
        description = WayangA2aMaps.optional(description);
        exchanges = exchanges == null || exchanges.isEmpty()
                ? List.of()
                : exchanges.stream().toList();
        if (exchanges.isEmpty()) {
            throw new IllegalArgumentException("A2A HTTP scenario requires at least one exchange");
        }
        attributes = WayangA2aMaps.copyMap(attributes);
    }

    public static WayangA2aHttpScenario of(String id, List<WayangA2aHttpRequest> requests) {
        return new WayangA2aHttpScenario(
                id,
                null,
                requests.stream().map(WayangA2aHttpScenarioExchange::of).toList(),
                Map.of());
    }

    public Map<String, Object> toMap() {
        return WayangA2aScenarioDefinitionProjection.definition(
                id,
                description,
                exchanges,
                WayangA2aHttpScenarioExchange::toMap,
                attributes);
    }
}

package tech.kayys.wayang.a2a.wayang;

import java.util.Map;

/**
 * One HTTP request in an A2A harness scenario.
 */
public record WayangA2aHttpScenarioExchange(
        WayangA2aHttpRequest request,
        Map<String, Object> attributes) {

    public WayangA2aHttpScenarioExchange {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        attributes = WayangA2aMaps.copyMap(attributes);
    }

    public static WayangA2aHttpScenarioExchange of(WayangA2aHttpRequest request) {
        return new WayangA2aHttpScenarioExchange(request, Map.of());
    }

    public Map<String, Object> toMap() {
        return WayangA2aScenarioExchangeDefinitionProjection.exchange(
                null,
                request.method(),
                request.path(),
                request.headers(),
                request.attributes(),
                null,
                attributes);
    }
}

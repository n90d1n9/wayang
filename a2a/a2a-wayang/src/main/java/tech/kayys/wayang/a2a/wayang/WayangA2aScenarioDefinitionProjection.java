package tech.kayys.wayang.a2a.wayang;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Shared ordered projection for A2A scenario definitions.
 */
final class WayangA2aScenarioDefinitionProjection {

    private WayangA2aScenarioDefinitionProjection() {
    }

    static <E> Map<String, Object> definition(
            String id,
            String description,
            List<E> exchanges,
            Function<? super E, Map<String, Object>> exchangeMapper,
            Map<String, Object> attributes) {
        List<Map<String, Object>> exchangeMaps = maps(exchanges, exchangeMapper);
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("id", WayangA2aMaps.required(id, "id"));
        String normalizedDescription = WayangA2aMaps.optional(description);
        if (normalizedDescription != null) {
            values.put("description", normalizedDescription);
        }
        values.put("exchangeCount", exchangeMaps.size());
        values.put("exchanges", exchangeMaps);
        values.put("attributes", WayangA2aMaps.copyMap(attributes));
        return WayangA2aMaps.copyMap(values);
    }

    private static <E> List<Map<String, Object>> maps(
            List<E> values,
            Function<? super E, Map<String, Object>> mapper) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .map(mapper)
                .map(WayangA2aMaps::copyMap)
                .toList();
    }
}

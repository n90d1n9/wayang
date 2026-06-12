package tech.kayys.wayang.a2a.wayang;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Shared ordered projection for A2A scenario run results.
 */
final class WayangA2aScenarioResultProjection {

    private WayangA2aScenarioResultProjection() {
    }

    static <E, I> Map<String, Object> result(
            String scenarioId,
            boolean passed,
            int exchangeCount,
            List<E> exchanges,
            Function<? super E, Map<String, Object>> exchangeMapper,
            List<I> issues,
            Function<? super I, Map<String, Object>> issueMapper,
            Map<String, Object> attributes) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("scenarioId", WayangA2aMaps.required(scenarioId, "scenarioId"));
        values.put("passed", passed);
        values.put("exchangeCount", exchangeCount);
        values.put("issueCount", issues == null ? 0 : issues.size());
        values.put("exchanges", maps(exchanges, exchangeMapper));
        values.put("issues", maps(issues, issueMapper));
        values.put("attributes", WayangA2aMaps.copyMap(attributes));
        return WayangA2aMaps.copyMap(values);
    }

    private static <T> List<Map<String, Object>> maps(
            List<T> values,
            Function<? super T, Map<String, Object>> mapper) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .map(mapper)
                .map(WayangA2aMaps::copyMap)
                .toList();
    }
}

package tech.kayys.wayang.a2a.wayang;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Ordered projection helpers for A2A JSON-RPC smoke run results.
 */
final class WayangA2aJsonRpcSmokeResultProjection {

    private WayangA2aJsonRpcSmokeResultProjection() {
    }

    static Map<String, Object> attributes(WayangA2aJsonRpcScenario scenario) {
        WayangA2aJsonRpcScenario resolved = Objects.requireNonNull(scenario, "scenario");
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("scenarioId", resolved.id());
        values.put("exchangeCount", resolved.exchanges().size());
        values.putAll(resolved.attributes());
        return WayangA2aMaps.copyMap(values);
    }

    static Map<String, Object> result(
            boolean passed,
            int exitCode,
            Map<String, Object> scenarioResult,
            Map<String, Object> attributes) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("passed", passed);
        values.put("exitCode", exitCode);
        values.put("scenarioResult", WayangA2aMaps.copyMap(scenarioResult));
        values.put("attributes", WayangA2aMaps.copyMap(attributes));
        return WayangA2aMaps.copyMap(values);
    }
}

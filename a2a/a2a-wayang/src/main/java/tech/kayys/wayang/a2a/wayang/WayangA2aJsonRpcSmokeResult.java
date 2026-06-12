package tech.kayys.wayang.a2a.wayang;

import java.util.Map;
import java.util.Objects;

/**
 * Operational result for one A2A JSON-RPC smoke run.
 */
public record WayangA2aJsonRpcSmokeResult(
        WayangA2aJsonRpcScenarioResult scenarioResult,
        Map<String, Object> attributes) {

    public static final int EXIT_SUCCESS = 0;
    public static final int EXIT_FAILURE = 1;

    public WayangA2aJsonRpcSmokeResult {
        scenarioResult = Objects.requireNonNull(scenarioResult, "scenarioResult");
        attributes = WayangA2aMaps.copyMap(attributes);
    }

    public boolean passed() {
        return scenarioResult.passed();
    }

    public int exitCode() {
        return passed() ? EXIT_SUCCESS : EXIT_FAILURE;
    }

    public Map<String, Object> toMap() {
        return WayangA2aJsonRpcSmokeResultProjection.result(
                passed(),
                exitCode(),
                scenarioResult.toMap(),
                attributes);
    }

    public String toJson() {
        return WayangA2aHttpJson.write(toMap());
    }
}

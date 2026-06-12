package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.wayang.http.HttpSmokeResultProjection;
import tech.kayys.wayang.a2ui.wayang.transport.TransportJson;
import tech.kayys.wayang.a2ui.wayang.transport.TransportMaps;

import java.util.Map;
import java.util.Objects;

/**
 * Operational result for one A2UI HTTP smoke run.
 */
public record WayangA2uiHttpSmokeResult(
        WayangA2uiHttpScenarioSuiteResult suiteResult,
        WayangA2uiHttpExpectationResult expectationResult,
        Map<String, Object> attributes) {

    public static final int EXIT_SUCCESS = 0;
    public static final int EXIT_FAILURE = 1;

    public WayangA2uiHttpSmokeResult {
        suiteResult = Objects.requireNonNull(suiteResult, "suiteResult");
        expectationResult = Objects.requireNonNull(expectationResult, "expectationResult");
        attributes = TransportMaps.copy(attributes);
    }

    public boolean passed() {
        return suiteResult.report().passed() && expectationResult.passed();
    }

    public int exitCode() {
        return passed() ? EXIT_SUCCESS : EXIT_FAILURE;
    }

    public Map<String, Object> toMap() {
        return HttpSmokeResultProjection.result(this);
    }

    public String toJson() {
        return TransportJson.json(toMap(), "Unable to encode A2UI HTTP smoke result");
    }
}

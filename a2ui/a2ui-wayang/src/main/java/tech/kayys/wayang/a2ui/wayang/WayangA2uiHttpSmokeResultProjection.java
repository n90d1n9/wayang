package tech.kayys.wayang.a2ui.wayang;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Ordered projection for A2UI HTTP smoke runner results.
 */
final class WayangA2uiHttpSmokeResultProjection {

    private WayangA2uiHttpSmokeResultProjection() {
    }

    static Map<String, Object> result(WayangA2uiHttpSmokeResult result) {
        WayangA2uiHttpSmokeResult resolved = Objects.requireNonNull(result, "result");
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("passed", resolved.passed());
        values.put("exitCode", resolved.exitCode());
        values.put("suiteReport", resolved.suiteResult().toMap());
        values.put("expectationResult", resolved.expectationResult().toMap());
        values.put("attributes", resolved.attributes());
        return WayangA2uiTransportMaps.freeze(values);
    }
}

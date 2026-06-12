package tech.kayys.wayang.a2a.wayang;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Parser and ordered projection helpers for A2A JSON-RPC diagnostic handler coverage.
 */
final class WayangA2aJsonRpcHttpDiagnosticHandlerCoverageProjection {

    private WayangA2aJsonRpcHttpDiagnosticHandlerCoverageProjection() {
    }

    static WayangA2aJsonRpcHttpDiagnosticHandlerCoverage fromMap(Map<?, ?> values) {
        Map<String, Object> copy = WayangA2aMaps.copyMap(values);
        return new WayangA2aJsonRpcHttpDiagnosticHandlerCoverage(
                reported(copy),
                WayangA2aMaps.stringList(copy.get("routeKeys")),
                WayangA2aMaps.stringList(copy.get("handlerKeys")),
                WayangA2aMaps.stringList(copy.get("missingHandlerKeys")),
                WayangA2aMaps.stringList(copy.get("orphanHandlerKeys")));
    }

    static Map<String, Object> coverage(WayangA2aJsonRpcHttpDiagnosticHandlerCoverage coverage) {
        WayangA2aJsonRpcHttpDiagnosticHandlerCoverage resolved =
                Objects.requireNonNull(coverage, "coverage");
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("complete", resolved.complete());
        values.put("routeKeyCount", resolved.routeKeyCount());
        values.put("handlerKeyCount", resolved.handlerKeyCount());
        values.put("routeKeys", resolved.routeKeys());
        values.put("handlerKeys", resolved.handlerKeys());
        values.put("missingHandlerKeys", resolved.missingHandlerKeys());
        values.put("orphanHandlerKeys", resolved.orphanHandlerKeys());
        return WayangA2aMaps.copyMap(values);
    }

    private static boolean reported(Map<String, Object> values) {
        return values.containsKey("routeKeys")
                || values.containsKey("handlerKeys")
                || values.containsKey("missingHandlerKeys")
                || values.containsKey("orphanHandlerKeys");
    }
}

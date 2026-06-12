package tech.kayys.wayang.a2a.wayang;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Ordered projection helpers for A2A JSON-RPC smoke probe envelopes.
 */
final class WayangA2aJsonRpcSmokeProbeProjection {

    private WayangA2aJsonRpcSmokeProbeProjection() {
    }

    static WayangA2aHttpResponse response(WayangA2aJsonRpcSmokeResult result) {
        WayangA2aJsonRpcSmokeResult resolved = Objects.requireNonNull(result, "result");
        return WayangA2aJsonRpcHttpResponses.json(
                        WayangA2aJsonRpcSmokeProbeResult.OPERATION_JSON_RPC_SMOKE,
                        resolved.toJson())
                .withHeaders(smokeHeaders(resolved));
    }

    static Map<String, Object> smokeHeaders(WayangA2aJsonRpcSmokeResult result) {
        WayangA2aJsonRpcSmokeResult resolved = Objects.requireNonNull(result, "result");
        Map<String, Object> headers = new LinkedHashMap<>();
        headers.put(WayangA2aJsonRpcSmokeProbeResult.HEADER_A2A_SMOKE_PASSED, resolved.passed());
        headers.put(WayangA2aJsonRpcSmokeProbeResult.HEADER_A2A_SMOKE_EXIT_CODE, resolved.exitCode());
        headers.put(WayangA2aJsonRpcSmokeProbeResult.HEADER_A2A_SMOKE_SCENARIO, scenarioId(resolved));
        return WayangA2aMaps.copyMap(headers);
    }

    static Map<String, Object> probe(
            int statusCode,
            boolean httpSuccessful,
            String routeOperation,
            String protocolVersion,
            String contentType,
            boolean smokeRoute,
            boolean passed,
            int exitCode,
            WayangA2aJsonRpcSmokeSummary summary,
            Map<String, Object> headers) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("statusCode", statusCode);
        values.put("httpSuccessful", httpSuccessful);
        values.put("routeOperation", routeOperation);
        values.put("protocolVersion", protocolVersion);
        values.put("contentType", contentType);
        values.put("smokeRoute", smokeRoute);
        values.put("passed", passed);
        values.put("exitCode", exitCode);
        values.put("summary", Objects.requireNonNull(summary, "summary").toMap());
        values.put("headers", WayangA2aMaps.copyMap(headers));
        return WayangA2aMaps.copyMap(values);
    }

    private static String scenarioId(WayangA2aJsonRpcSmokeResult result) {
        String scenarioId = WayangA2aMaps.optional(result.attributes().get("scenarioId"));
        return scenarioId == null ? "" : scenarioId;
    }
}

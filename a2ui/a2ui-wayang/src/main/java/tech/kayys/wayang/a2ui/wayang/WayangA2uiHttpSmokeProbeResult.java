package tech.kayys.wayang.a2ui.wayang;

import java.util.Map;
import java.util.Objects;

/**
 * HTTP-aware result for callers probing the A2UI smoke endpoint.
 */
public record WayangA2uiHttpSmokeProbeResult(
        int statusCode,
        boolean httpSuccessful,
        String routeOperation,
        String allow,
        String outcome,
        WayangA2uiHttpSmokeSummary summary,
        Map<String, Object> headers) {

    public WayangA2uiHttpSmokeProbeResult {
        statusCode = Math.max(0, statusCode);
        routeOperation = routeOperation == null ? "" : routeOperation.trim();
        allow = allow == null ? "" : allow.trim();
        outcome = outcome == null ? "" : outcome.trim();
        summary = Objects.requireNonNull(summary, "summary");
        headers = WayangA2uiTransportMaps.copy(headers);
    }

    public static WayangA2uiHttpSmokeProbeResult run(WayangA2uiHttpBridgeAdapter adapter) {
        WayangA2uiHttpBridgeAdapter resolved = Objects.requireNonNull(adapter, "adapter");
        return from(resolved.smoke());
    }

    public static WayangA2uiHttpSmokeProbeResult from(WayangA2uiHttpResponse response) {
        WayangA2uiHttpResponse resolved = Objects.requireNonNull(response, "response");
        return new WayangA2uiHttpSmokeProbeResult(
                resolved.statusCode(),
                resolved.successful(),
                header(resolved, WayangA2uiHttpResponse.HEADER_A2UI_ROUTE_OPERATION),
                header(resolved, WayangA2uiHttpResponse.HEADER_ALLOW),
                header(resolved, WayangA2uiHttpResponse.HEADER_A2UI_OUTCOME),
                WayangA2uiHttpSmokeSummary.from(resolved),
                resolved.headers());
    }

    public static WayangA2uiHttpSmokeProbeResult fromMap(Map<?, ?> values) {
        return WayangA2uiHttpSmokeProbeResultDecoder.fromMap(values);
    }

    public static WayangA2uiHttpSmokeProbeResult fromJson(String json) {
        return WayangA2uiHttpSmokeProbeResultDecoder.fromJson(json);
    }

    public boolean smokeRoute() {
        return WayangA2uiHttpRoute.OPERATION_SMOKE.equals(routeOperation);
    }

    public boolean passed() {
        return httpSuccessful && smokeRoute() && summary.successfulExit();
    }

    public int exitCode() {
        if (passed()) {
            return WayangA2uiHttpSmokeResult.EXIT_SUCCESS;
        }
        return summary.exitCode() == WayangA2uiHttpSmokeResult.EXIT_SUCCESS
                ? WayangA2uiHttpSmokeResult.EXIT_FAILURE
                : summary.exitCode();
    }

    public Map<String, Object> toMap() {
        return WayangA2uiHttpSmokeProbeProjection.probe(this);
    }

    public String toJson() {
        return WayangA2uiTransportJson.json(toMap(), "Unable to encode A2UI HTTP smoke probe result");
    }

    private static String header(WayangA2uiHttpResponse response, String name) {
        Object value = response.headers().get(name);
        return value == null ? "" : String.valueOf(value);
    }

}

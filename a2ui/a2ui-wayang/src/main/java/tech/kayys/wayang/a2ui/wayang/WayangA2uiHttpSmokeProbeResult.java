package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.wayang.http.HttpSmokeProbeProjection;
import tech.kayys.wayang.a2ui.wayang.http.HttpSmokeProbeResponseDecoder;
import tech.kayys.wayang.a2ui.wayang.transport.TransportJson;
import tech.kayys.wayang.a2ui.wayang.transport.TransportMaps;

import tech.kayys.wayang.a2ui.wayang.support.RecordValues;
import tech.kayys.wayang.a2ui.wayang.support.RecordNumbers;

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
        statusCode = RecordNumbers.nonNegative(statusCode);
        routeOperation = RecordValues.text(routeOperation);
        allow = RecordValues.text(allow);
        outcome = RecordValues.text(outcome);
        summary = Objects.requireNonNull(summary, "summary");
        headers = TransportMaps.copy(headers);
    }

    public static WayangA2uiHttpSmokeProbeResult run(WayangA2uiHttpBridgeAdapter adapter) {
        WayangA2uiHttpBridgeAdapter resolved = Objects.requireNonNull(adapter, "adapter");
        return from(resolved.smoke());
    }

    public static WayangA2uiHttpSmokeProbeResult from(WayangA2uiHttpResponse response) {
        return HttpSmokeProbeResponseDecoder.from(response);
    }

    public static WayangA2uiHttpSmokeProbeResult fromMap(Map<?, ?> values) {
        return WayangA2uiHttpSmokeProbeResultDecoder.fromMap(values);
    }

    public static WayangA2uiHttpSmokeProbeResult fromJson(String json) {
        return WayangA2uiHttpSmokeProbeResultDecoder.fromJson(json);
    }

    public static WayangA2uiHttpSmokeProbeResult empty() {
        return new WayangA2uiHttpSmokeProbeResult(
                0,
                false,
                "",
                "",
                "",
                WayangA2uiHttpSmokeSummary.empty(),
                Map.of());
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
        return HttpSmokeProbeProjection.probe(this);
    }

    public String toJson() {
        return TransportJson.json(toMap(), "Unable to encode A2UI HTTP smoke probe result");
    }

}

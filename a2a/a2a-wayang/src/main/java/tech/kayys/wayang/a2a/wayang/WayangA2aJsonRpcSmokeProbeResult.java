package tech.kayys.wayang.a2a.wayang;

import java.util.Map;
import java.util.Objects;

import static tech.kayys.wayang.a2a.wayang.WayangA2aJsonReportMaps.bodyMap;
import static tech.kayys.wayang.a2a.wayang.WayangA2aJsonReportMaps.bool;
import static tech.kayys.wayang.a2a.wayang.WayangA2aJsonReportMaps.map;
import static tech.kayys.wayang.a2a.wayang.WayangA2aJsonReportMaps.number;
import static tech.kayys.wayang.a2a.wayang.WayangA2aJsonReportMaps.text;
import static tech.kayys.wayang.a2a.wayang.WayangA2aHttpResponseHeaders.protocolVersionHeader;
import static tech.kayys.wayang.a2a.wayang.WayangA2aHttpResponseHeaders.routeOperationHeader;

/**
 * HTTP-aware result for callers probing the A2A JSON-RPC smoke surface.
 */
public record WayangA2aJsonRpcSmokeProbeResult(
        int statusCode,
        boolean httpSuccessful,
        String routeOperation,
        String protocolVersion,
        String contentType,
        WayangA2aJsonRpcSmokeSummary summary,
        Map<String, Object> headers) {

    public static final String OPERATION_JSON_RPC_SMOKE = "JsonRpcSmoke";
    public static final String HEADER_A2A_SMOKE_PASSED = "X-Wayang-A2A-Smoke-Passed";
    public static final String HEADER_A2A_SMOKE_EXIT_CODE = "X-Wayang-A2A-Smoke-Exit-Code";
    public static final String HEADER_A2A_SMOKE_SCENARIO = "X-Wayang-A2A-Smoke-Scenario";

    public WayangA2aJsonRpcSmokeProbeResult {
        statusCode = Math.max(0, statusCode);
        routeOperation = routeOperation == null ? "" : routeOperation.trim();
        protocolVersion = protocolVersion == null ? "" : protocolVersion.trim();
        contentType = contentType == null ? "" : contentType.trim();
        summary = Objects.requireNonNull(summary, "summary");
        headers = WayangA2aMaps.copyMap(headers);
    }

    public static WayangA2aJsonRpcSmokeProbeResult run(WayangA2aJsonRpcSmokeRunner runner) {
        return from(response(Objects.requireNonNull(runner, "runner").run()));
    }

    public static WayangA2aJsonRpcSmokeProbeResult run(WayangA2aJsonRpcHttpAdapter adapter) {
        return from(Objects.requireNonNull(adapter, "adapter").smokeResponse());
    }

    public static WayangA2aJsonRpcSmokeProbeResult from(WayangA2aHttpResponse response) {
        WayangA2aHttpResponse resolved = Objects.requireNonNull(response, "response");
        return new WayangA2aJsonRpcSmokeProbeResult(
                resolved.statusCode(),
                resolved.successful(),
                routeOperationHeader(resolved),
                protocolVersionHeader(resolved),
                resolved.contentType(),
                WayangA2aJsonRpcSmokeSummary.fromResultJson(resolved.body()),
                resolved.headers());
    }

    public static WayangA2aJsonRpcSmokeProbeResult fromMap(Map<?, ?> values) {
        Map<String, Object> copy = WayangA2aMaps.copyMap(values);
        return new WayangA2aJsonRpcSmokeProbeResult(
                number(copy.get("statusCode"), 0),
                bool(copy.get("httpSuccessful"), false),
                text(copy.get("routeOperation"), ""),
                text(copy.get("protocolVersion"), ""),
                text(copy.get("contentType"), ""),
                WayangA2aJsonRpcSmokeSummary.fromMap(map(copy.get("summary"))),
                map(copy.get("headers")));
    }

    public static WayangA2aJsonRpcSmokeProbeResult fromJson(String json) {
        return fromMap(bodyMap(json));
    }

    public static WayangA2aHttpResponse response(WayangA2aJsonRpcSmokeResult result) {
        return WayangA2aJsonRpcSmokeProbeProjection.response(result);
    }

    public boolean smokeRoute() {
        return OPERATION_JSON_RPC_SMOKE.equals(routeOperation);
    }

    public boolean passed() {
        return httpSuccessful && smokeRoute() && summary.successfulExit();
    }

    public int exitCode() {
        if (passed()) {
            return WayangA2aJsonRpcSmokeResult.EXIT_SUCCESS;
        }
        return summary.exitCode() == WayangA2aJsonRpcSmokeResult.EXIT_SUCCESS
                ? WayangA2aJsonRpcSmokeResult.EXIT_FAILURE
                : summary.exitCode();
    }

    public Map<String, Object> toMap() {
        return WayangA2aJsonRpcSmokeProbeProjection.probe(
                statusCode,
                httpSuccessful,
                routeOperation,
                protocolVersion,
                contentType,
                smokeRoute(),
                passed(),
                exitCode(),
                summary,
                headers);
    }

    public String toJson() {
        return WayangA2aHttpJson.write(toMap());
    }

}

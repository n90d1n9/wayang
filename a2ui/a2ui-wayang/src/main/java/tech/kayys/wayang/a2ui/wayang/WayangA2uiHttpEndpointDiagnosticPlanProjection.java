package tech.kayys.wayang.a2ui.wayang;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Ordered projection helpers for A2UI HTTP endpoint diagnostic plans.
 */
final class WayangA2uiHttpEndpointDiagnosticPlanProjection {

    private WayangA2uiHttpEndpointDiagnosticPlanProjection() {
    }

    static Map<String, Object> config(WayangA2uiHttpEndpointDiagnosticConfig config) {
        WayangA2uiHttpEndpointDiagnosticConfig resolved = Objects.requireNonNull(config, "config");
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(WayangA2uiHttpEndpointDiagnosticConfig.KEY_ROUTE_CATALOG_PROBE, resolved.routeCatalogProbe());
        values.put(WayangA2uiHttpEndpointDiagnosticConfig.KEY_BINDING_REPORT_PROBE, resolved.bindingReportProbe());
        values.put(WayangA2uiHttpEndpointDiagnosticConfig.KEY_SMOKE_PROBE, resolved.smokeProbe());
        values.put(WayangA2uiHttpEndpointDiagnosticConfig.KEY_READINESS_PROBE, resolved.readinessProbe());
        values.put(WayangA2uiHttpEndpointDiagnosticConfig.KEY_ROUTE_OPTIONS_PROBE, resolved.routeOptionsProbe());
        values.put(WayangA2uiHttpEndpointDiagnosticConfig.KEY_DEFAULT_HEADERS, resolved.defaultHeaders());
        values.put(WayangA2uiHttpEndpointDiagnosticConfig.KEY_DEFAULT_ATTRIBUTES, resolved.defaultAttributes());
        return WayangA2uiTransportMaps.freeze(values);
    }

    static Map<String, Object> plan(WayangA2uiHttpEndpointDiagnosticPlan plan) {
        WayangA2uiHttpEndpointDiagnosticPlan resolved = Objects.requireNonNull(plan, "plan");
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(WayangA2uiHttpEndpointDiagnosticPlan.KEY_DIAGNOSTICS_ID, resolved.diagnosticsId());
        values.put(WayangA2uiHttpEndpointDiagnosticPlan.KEY_CONFIG, resolved.config().toMap());
        values.put(WayangA2uiHttpEndpointDiagnosticPlan.KEY_REQUESTS, resolved.requests().stream()
                .map(WayangA2uiHttpEndpointDiagnosticRequest::toMap)
                .toList());
        values.put("requestCount", resolved.requestCount());
        values.put("usesDefaultRequests", resolved.usesDefaultRequests());
        values.put(WayangA2uiHttpEndpointDiagnosticPlan.KEY_ATTRIBUTES, resolved.attributes());
        return WayangA2uiTransportMaps.freeze(values);
    }

    static Map<String, Object> request(WayangA2uiHttpEndpointDiagnosticRequest request) {
        WayangA2uiHttpEndpointDiagnosticRequest resolved = Objects.requireNonNull(request, "request");
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(WayangA2uiHttpEndpointDiagnosticRequest.KEY_METHOD, resolved.method());
        values.put(WayangA2uiHttpEndpointDiagnosticRequest.KEY_RAW_PATH, resolved.rawPath());
        values.put(WayangA2uiHttpEndpointDiagnosticRequest.KEY_BODY, resolved.body());
        values.put("bodyPresent", !resolved.body().isBlank());
        values.put("bodyLength", resolved.body().length());
        values.put(WayangA2uiHttpEndpointDiagnosticRequest.KEY_HEADERS, resolved.headers());
        values.put(WayangA2uiHttpEndpointDiagnosticRequest.KEY_ATTRIBUTES, resolved.attributes());
        return WayangA2uiTransportMaps.freeze(values);
    }
}

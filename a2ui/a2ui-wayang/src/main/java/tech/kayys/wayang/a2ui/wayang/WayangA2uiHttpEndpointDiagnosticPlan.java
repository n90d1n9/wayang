package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.wayang.http.HttpEndpointDiagnosticPlanProjection;
import tech.kayys.wayang.a2ui.wayang.transport.TransportJson;
import tech.kayys.wayang.a2ui.wayang.transport.TransportMaps;

import tech.kayys.wayang.a2ui.wayang.support.RecordValues;
import tech.kayys.wayang.a2ui.wayang.support.RecordCollections;

import java.util.List;
import java.util.Map;

/**
 * JSON-friendly payload for one mounted A2UI endpoint diagnostics run.
 */
public record WayangA2uiHttpEndpointDiagnosticPlan(
        String diagnosticsId,
        WayangA2uiHttpEndpointDiagnosticConfig config,
        List<WayangA2uiHttpEndpointDiagnosticRequest> requests,
        Map<String, Object> attributes) {

    public static final String KEY_DIAGNOSTICS_ID = "diagnosticsId";
    public static final String KEY_ID = "id";
    public static final String KEY_CONFIG = "config";
    public static final String KEY_REQUESTS = "requests";
    public static final String KEY_ATTRIBUTES = "attributes";

    public WayangA2uiHttpEndpointDiagnosticPlan {
        diagnosticsId = RecordValues.textOrDefault(
                diagnosticsId,
                WayangA2uiHttpEndpointDiagnostics.DEFAULT_ID);
        config = config == null ? WayangA2uiHttpEndpointDiagnosticConfig.defaultConfig() : config;
        requests = RecordCollections.nonNullList(requests);
        attributes = TransportMaps.copy(attributes);
    }

    public static WayangA2uiHttpEndpointDiagnosticPlan defaultPlan() {
        return new WayangA2uiHttpEndpointDiagnosticPlan(
                WayangA2uiHttpEndpointDiagnostics.DEFAULT_ID,
                WayangA2uiHttpEndpointDiagnosticConfig.defaultConfig(),
                List.of(),
                Map.of());
    }

    public static WayangA2uiHttpEndpointDiagnosticPlan defaults() {
        return defaultPlan();
    }

    public static WayangA2uiHttpEndpointDiagnosticPlan fromMap(Map<?, ?> values) {
        return WayangA2uiHttpEndpointDiagnosticPlanDecoder.fromMap(values);
    }

    public static WayangA2uiHttpEndpointDiagnosticPlan fromJson(String json) {
        return WayangA2uiHttpEndpointDiagnosticPlanDecoder.fromJson(json);
    }

    public boolean usesDefaultRequests() {
        return requests.isEmpty();
    }

    public int requestCount() {
        return requests.size();
    }

    public Map<String, Object> toMap() {
        return HttpEndpointDiagnosticPlanProjection.plan(this);
    }

    public String toJson() {
        return TransportJson.json(toMap(), "Unable to encode A2UI HTTP endpoint diagnostic plan");
    }

}

package tech.kayys.wayang.a2ui.wayang;

import java.util.List;
import java.util.Map;
import java.util.Objects;

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
        diagnosticsId = diagnosticsId == null || diagnosticsId.isBlank()
                ? WayangA2uiHttpEndpointDiagnostics.DEFAULT_ID
                : diagnosticsId.trim();
        config = config == null ? WayangA2uiHttpEndpointDiagnosticConfig.defaults() : config;
        requests = requests == null
                ? List.of()
                : requests.stream()
                        .filter(Objects::nonNull)
                        .toList();
        attributes = WayangA2uiTransportMaps.copy(attributes);
    }

    public static WayangA2uiHttpEndpointDiagnosticPlan defaults() {
        return new WayangA2uiHttpEndpointDiagnosticPlan(
                WayangA2uiHttpEndpointDiagnostics.DEFAULT_ID,
                WayangA2uiHttpEndpointDiagnosticConfig.defaults(),
                List.of(),
                Map.of());
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
        return WayangA2uiHttpEndpointDiagnosticPlanProjection.plan(this);
    }

    public String toJson() {
        return WayangA2uiTransportJson.json(toMap(), "Unable to encode A2UI HTTP endpoint diagnostic plan");
    }

}

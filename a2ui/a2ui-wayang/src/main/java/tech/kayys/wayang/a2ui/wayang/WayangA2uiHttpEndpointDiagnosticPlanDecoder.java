package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.wayang.transport.TransportJson;
import tech.kayys.wayang.a2ui.wayang.transport.TransportMaps;

import tech.kayys.wayang.a2ui.wayang.support.DecodeValues;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Canonical decoder for stored or remote A2UI HTTP endpoint diagnostic plans.
 */
public final class WayangA2uiHttpEndpointDiagnosticPlanDecoder {

    public static WayangA2uiHttpEndpointDiagnosticPlan fromMap(Map<?, ?> values) {
        if (values == null || values.isEmpty()) {
            return WayangA2uiHttpEndpointDiagnosticPlan.defaultPlan();
        }
        Map<String, Object> plan = TransportMaps.copy(values);
        return new WayangA2uiHttpEndpointDiagnosticPlan(
                diagnosticsId(plan),
                config(plan),
                requests(plan.get(WayangA2uiHttpEndpointDiagnosticPlan.KEY_REQUESTS)),
                TransportMaps.copyMap(
                        plan.get(WayangA2uiHttpEndpointDiagnosticPlan.KEY_ATTRIBUTES)));
    }

    public static WayangA2uiHttpEndpointDiagnosticPlan fromJson(String json) {
        return fromMap(TransportJson.map(
                json,
                "A2UI HTTP endpoint diagnostic plan JSON must not be blank",
                "Unable to decode A2UI HTTP endpoint diagnostic plan JSON"));
    }

    private static String diagnosticsId(Map<String, Object> values) {
        String diagnosticsId = DecodeValues.text(
                values.get(WayangA2uiHttpEndpointDiagnosticPlan.KEY_DIAGNOSTICS_ID));
        return diagnosticsId.isBlank()
                ? DecodeValues.text(values.get(WayangA2uiHttpEndpointDiagnosticPlan.KEY_ID))
                : diagnosticsId;
    }

    private static WayangA2uiHttpEndpointDiagnosticConfig config(Map<String, Object> values) {
        if (values.get(WayangA2uiHttpEndpointDiagnosticPlan.KEY_CONFIG) instanceof Map<?, ?> config) {
            return WayangA2uiHttpEndpointDiagnosticConfig.fromMap(config);
        }
        Map<String, Object> config = new LinkedHashMap<>();
        copyIfPresent(config, values, WayangA2uiHttpEndpointDiagnosticConfig.KEY_PROFILE);
        copyIfPresent(config, values, WayangA2uiHttpEndpointDiagnosticConfig.KEY_PROBES);
        copyIfPresent(config, values, WayangA2uiHttpEndpointDiagnosticConfig.KEY_ROUTE_CATALOG_PROBE);
        copyIfPresent(config, values, WayangA2uiHttpEndpointDiagnosticConfig.KEY_BINDING_REPORT_PROBE);
        copyIfPresent(config, values, WayangA2uiHttpEndpointDiagnosticConfig.KEY_SMOKE_PROBE);
        copyIfPresent(config, values, WayangA2uiHttpEndpointDiagnosticConfig.KEY_READINESS_PROBE);
        copyIfPresent(config, values, WayangA2uiHttpEndpointDiagnosticConfig.KEY_ROUTE_OPTIONS_PROBE);
        copyIfPresent(config, values, WayangA2uiHttpEndpointDiagnosticConfig.KEY_DEFAULT_HEADERS);
        copyIfPresent(config, values, WayangA2uiHttpEndpointDiagnosticConfig.KEY_DEFAULT_ATTRIBUTES);
        copyIfPresent(config, values, "headers");
        return config.isEmpty()
                ? WayangA2uiHttpEndpointDiagnosticConfig.defaultConfig()
                : WayangA2uiHttpEndpointDiagnosticConfig.fromMap(config);
    }

    private static List<WayangA2uiHttpEndpointDiagnosticRequest> requests(Object value) {
        if (!(value instanceof List<?> list) || list.isEmpty()) {
            return List.of();
        }
        return list.stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .map(WayangA2uiHttpEndpointDiagnosticRequest::fromMap)
                .toList();
    }

    private static void copyIfPresent(
            Map<String, Object> target,
            Map<String, Object> source,
            String key) {
        if (source.containsKey(key)) {
            target.put(key, source.get(key));
        }
    }

    private WayangA2uiHttpEndpointDiagnosticPlanDecoder() {
    }
}

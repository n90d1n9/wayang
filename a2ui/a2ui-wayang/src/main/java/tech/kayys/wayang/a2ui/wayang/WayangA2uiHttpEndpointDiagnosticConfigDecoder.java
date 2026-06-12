package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.wayang.transport.TransportMaps;

import tech.kayys.wayang.a2ui.wayang.support.DecodeValues;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Canonical decoder for stored or remote A2UI HTTP endpoint diagnostic config.
 */
public final class WayangA2uiHttpEndpointDiagnosticConfigDecoder {

    public static WayangA2uiHttpEndpointDiagnosticConfig fromMap(Map<?, ?> values) {
        if (values == null || values.isEmpty()) {
            return WayangA2uiHttpEndpointDiagnosticConfig.defaultConfig();
        }
        Map<String, Object> config = mergeProbeMap(TransportMaps.copy(values));
        WayangA2uiHttpEndpointDiagnosticConfig base = baseProfile(
                DecodeValues.text(config.get(WayangA2uiHttpEndpointDiagnosticConfig.KEY_PROFILE)));
        return new WayangA2uiHttpEndpointDiagnosticConfig(
                bool(
                        config,
                        WayangA2uiHttpEndpointDiagnosticConfig.KEY_ROUTE_CATALOG_PROBE,
                        "routeCatalog",
                        base.routeCatalogProbe()),
                bool(
                        config,
                        WayangA2uiHttpEndpointDiagnosticConfig.KEY_BINDING_REPORT_PROBE,
                        "bindingReport",
                        base.bindingReportProbe()),
                bool(
                        config,
                        WayangA2uiHttpEndpointDiagnosticConfig.KEY_SMOKE_PROBE,
                        "smoke",
                        base.smokeProbe()),
                bool(
                        config,
                        WayangA2uiHttpEndpointDiagnosticConfig.KEY_READINESS_PROBE,
                        "readiness",
                        base.readinessProbe()),
                bool(
                        config,
                        WayangA2uiHttpEndpointDiagnosticConfig.KEY_ROUTE_OPTIONS_PROBE,
                        "routeOptions",
                        base.routeOptionsProbe()),
                map(config, WayangA2uiHttpEndpointDiagnosticConfig.KEY_DEFAULT_HEADERS, "headers"),
                map(config, WayangA2uiHttpEndpointDiagnosticConfig.KEY_DEFAULT_ATTRIBUTES, "attributes"));
    }

    private static WayangA2uiHttpEndpointDiagnosticConfig baseProfile(String profile) {
        return switch (normalizeProfile(profile)) {
            case WayangA2uiHttpEndpointDiagnosticConfig.PROFILE_DISCOVERY_ONLY ->
                    WayangA2uiHttpEndpointDiagnosticConfig.discoveryOnly();
            default -> WayangA2uiHttpEndpointDiagnosticConfig.defaultConfig();
        };
    }

    private static Map<String, Object> mergeProbeMap(Map<String, Object> config) {
        Object probes = config.get(WayangA2uiHttpEndpointDiagnosticConfig.KEY_PROBES);
        if (!(probes instanceof Map<?, ?> probeMap) || probeMap.isEmpty()) {
            return config;
        }
        Map<String, Object> merged = new LinkedHashMap<>(TransportMaps.copy(probeMap));
        config.forEach((key, value) -> {
            if (!WayangA2uiHttpEndpointDiagnosticConfig.KEY_PROBES.equals(key)) {
                merged.put(key, value);
            }
        });
        return TransportMaps.freeze(merged);
    }

    private static boolean bool(
            Map<String, Object> config,
            String key,
            String alias,
            boolean fallback) {
        if (config.containsKey(key)) {
            return DecodeValues.bool(config.get(key), fallback);
        }
        if (config.containsKey(alias)) {
            return DecodeValues.bool(config.get(alias), fallback);
        }
        return fallback;
    }

    private static Map<String, Object> map(Map<String, Object> config, String key, String alias) {
        if (config.get(key) instanceof Map<?, ?> map) {
            return TransportMaps.copy(map);
        }
        if (config.get(alias) instanceof Map<?, ?> map) {
            return TransportMaps.copy(map);
        }
        return Map.of();
    }

    private static String normalizeProfile(String profile) {
        return switch (profile.toLowerCase(Locale.ROOT)) {
            case "discovery", "discovery_only", "discovery-only" ->
                    WayangA2uiHttpEndpointDiagnosticConfig.PROFILE_DISCOVERY_ONLY;
            default -> WayangA2uiHttpEndpointDiagnosticConfig.PROFILE_DEFAULT;
        };
    }

    private WayangA2uiHttpEndpointDiagnosticConfigDecoder() {
    }
}

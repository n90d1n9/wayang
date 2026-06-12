package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.wayang.http.HttpEndpointDiagnosticPlanProjection;
import tech.kayys.wayang.a2ui.wayang.transport.TransportMaps;

import java.util.Map;

/**
 * Configuration for mounted A2UI endpoint diagnostics.
 */
public record WayangA2uiHttpEndpointDiagnosticConfig(
        boolean routeCatalogProbe,
        boolean bindingReportProbe,
        boolean smokeProbe,
        boolean readinessProbe,
        boolean routeOptionsProbe,
        Map<String, Object> defaultHeaders,
        Map<String, Object> defaultAttributes) {

    public static final String KEY_PROFILE = "profile";
    public static final String KEY_PROBES = "probes";
    public static final String KEY_ROUTE_CATALOG_PROBE = "routeCatalogProbe";
    public static final String KEY_BINDING_REPORT_PROBE = "bindingReportProbe";
    public static final String KEY_SMOKE_PROBE = "smokeProbe";
    public static final String KEY_READINESS_PROBE = "readinessProbe";
    public static final String KEY_ROUTE_OPTIONS_PROBE = "routeOptionsProbe";
    public static final String KEY_DEFAULT_HEADERS = "defaultHeaders";
    public static final String KEY_DEFAULT_ATTRIBUTES = "defaultAttributes";

    public static final String PROFILE_DEFAULT = "default";
    public static final String PROFILE_DISCOVERY_ONLY = "discovery-only";

    public WayangA2uiHttpEndpointDiagnosticConfig {
        defaultHeaders = TransportMaps.copy(defaultHeaders);
        defaultAttributes = TransportMaps.copy(defaultAttributes);
    }

    public static WayangA2uiHttpEndpointDiagnosticConfig defaultConfig() {
        return new WayangA2uiHttpEndpointDiagnosticConfig(
                true,
                true,
                true,
                true,
                true,
                Map.of(),
                Map.of());
    }

    public static WayangA2uiHttpEndpointDiagnosticConfig defaults() {
        return defaultConfig();
    }

    public static WayangA2uiHttpEndpointDiagnosticConfig discoveryOnly() {
        return new WayangA2uiHttpEndpointDiagnosticConfig(
                true,
                true,
                false,
                false,
                false,
                Map.of(),
                Map.of());
    }

    public static WayangA2uiHttpEndpointDiagnosticConfig fromMap(Map<?, ?> values) {
        return WayangA2uiHttpEndpointDiagnosticConfigDecoder.fromMap(values);
    }

    public WayangA2uiHttpEndpointDiagnosticConfig withDefaultHeaders(Map<?, ?> extraHeaders) {
        if (extraHeaders == null || extraHeaders.isEmpty()) {
            return this;
        }
        return new WayangA2uiHttpEndpointDiagnosticConfig(
                routeCatalogProbe,
                bindingReportProbe,
                smokeProbe,
                readinessProbe,
                routeOptionsProbe,
                WayangA2uiTransportMetadata.merge(defaultHeaders, TransportMaps.copy(extraHeaders)),
                defaultAttributes);
    }

    public WayangA2uiHttpEndpointDiagnosticConfig withDefaultAttributes(Map<?, ?> extraAttributes) {
        if (extraAttributes == null || extraAttributes.isEmpty()) {
            return this;
        }
        return new WayangA2uiHttpEndpointDiagnosticConfig(
                routeCatalogProbe,
                bindingReportProbe,
                smokeProbe,
                readinessProbe,
                routeOptionsProbe,
                defaultHeaders,
                WayangA2uiTransportMetadata.merge(defaultAttributes, TransportMaps.copy(extraAttributes)));
    }

    public Map<String, Object> toMap() {
        return HttpEndpointDiagnosticPlanProjection.config(this);
    }

}

package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.wayang.transport.TransportMetadataProjection;

import java.util.Map;

/**
 * Canonical metadata builders for Wayang A2UI transport envelopes.
 */
public final class WayangA2uiTransportMetadata {

    public static Map<String, Object> request(WayangA2uiTransportPayloadKind kind) {
        return TransportMetadataProjection.request(kind);
    }

    public static Map<String, Object> sessionResult(WayangA2uiSessionResult result) {
        return TransportMetadataProjection.sessionResult(result);
    }

    public static Map<String, Object> surfaceCatalog(WayangA2uiSurfaceCatalog catalog) {
        return TransportMetadataProjection.surfaceCatalog(catalog);
    }

    public static Map<String, Object> actionBindingReport(WayangA2uiActionBindingReport report) {
        return TransportMetadataProjection.actionBindingReport(report);
    }

    public static Map<String, Object> httpRouteCatalog(WayangA2uiHttpRouteCatalog catalog) {
        return TransportMetadataProjection.httpRouteCatalog(catalog);
    }

    public static Map<String, Object> httpBindingReport(WayangA2uiHttpBindingReport report) {
        return TransportMetadataProjection.httpBindingReport(report);
    }

    public static Map<String, Object> httpSmokeResult(WayangA2uiHttpSmokeResult result) {
        return TransportMetadataProjection.httpSmokeResult(result);
    }

    public static Map<String, Object> httpReadinessProbe(WayangA2uiHttpReadinessProbeResult result) {
        return TransportMetadataProjection.httpReadinessProbe(result);
    }

    public static Map<String, Object> error(WayangA2uiTransportError error) {
        return TransportMetadataProjection.error(error);
    }

    public static Map<String, Object> merge(Map<String, ?> metadata, Map<String, ?> extraMetadata) {
        return TransportMetadataProjection.merge(metadata, extraMetadata);
    }

    private WayangA2uiTransportMetadata() {
    }
}

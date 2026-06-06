package tech.kayys.wayang.a2ui.wayang;

import java.util.Map;

/**
 * Canonical metadata builders for Wayang A2UI transport envelopes.
 */
public final class WayangA2uiTransportMetadata {

    public static Map<String, Object> request(WayangA2uiTransportPayloadKind kind) {
        return WayangA2uiTransportMetadataProjection.request(kind);
    }

    public static Map<String, Object> sessionResult(WayangA2uiSessionResult result) {
        return WayangA2uiTransportMetadataProjection.sessionResult(result);
    }

    public static Map<String, Object> surfaceCatalog(WayangA2uiSurfaceCatalog catalog) {
        return WayangA2uiTransportMetadataProjection.surfaceCatalog(catalog);
    }

    public static Map<String, Object> httpRouteCatalog(WayangA2uiHttpRouteCatalog catalog) {
        return WayangA2uiTransportMetadataProjection.httpRouteCatalog(catalog);
    }

    public static Map<String, Object> httpBindingReport(WayangA2uiHttpBindingReport report) {
        return WayangA2uiTransportMetadataProjection.httpBindingReport(report);
    }

    public static Map<String, Object> httpSmokeResult(WayangA2uiHttpSmokeResult result) {
        return WayangA2uiTransportMetadataProjection.httpSmokeResult(result);
    }

    public static Map<String, Object> httpReadinessProbe(WayangA2uiHttpReadinessProbeResult result) {
        return WayangA2uiTransportMetadataProjection.httpReadinessProbe(result);
    }

    public static Map<String, Object> error(WayangA2uiTransportError error) {
        return WayangA2uiTransportMetadataProjection.error(error);
    }

    public static Map<String, Object> merge(Map<String, ?> metadata, Map<String, ?> extraMetadata) {
        return WayangA2uiTransportMetadataProjection.merge(metadata, extraMetadata);
    }

    private WayangA2uiTransportMetadata() {
    }
}

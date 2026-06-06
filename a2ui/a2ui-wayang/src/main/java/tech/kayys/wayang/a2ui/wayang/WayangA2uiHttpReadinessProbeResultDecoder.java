package tech.kayys.wayang.a2ui.wayang;

import java.util.Map;

/**
 * Canonical decoder for stored or remote A2UI HTTP readiness probe results.
 */
public final class WayangA2uiHttpReadinessProbeResultDecoder {

    public static WayangA2uiHttpReadinessProbeResult fromMap(Map<?, ?> values) {
        Map<String, Object> copy = WayangA2uiTransportMaps.copy(values);
        return new WayangA2uiHttpReadinessProbeResult(
                WayangA2uiHttpBindingReportProbeResult.fromMap(
                        WayangA2uiTransportMaps.copyMap(copy.get("bindingReportProbe"))),
                WayangA2uiHttpSmokeProbeResult.fromMap(WayangA2uiTransportMaps.copyMap(copy.get("smokeProbe"))),
                WayangA2uiDecodeValues.bool(copy.get("smokeRequired"), false));
    }

    public static WayangA2uiHttpReadinessProbeResult fromJson(String json) {
        return fromMap(WayangA2uiTransportJson.map(
                json,
                "A2UI HTTP readiness probe result JSON must not be blank",
                "Unable to decode A2UI HTTP readiness probe result JSON"));
    }

    private WayangA2uiHttpReadinessProbeResultDecoder() {
    }
}

package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.wayang.transport.TransportJson;
import tech.kayys.wayang.a2ui.wayang.transport.TransportMaps;

import tech.kayys.wayang.a2ui.wayang.support.DecodeValues;

import java.util.Map;

/**
 * Canonical decoder for stored or remote A2UI HTTP readiness probe results.
 */
public final class WayangA2uiHttpReadinessProbeResultDecoder {

    public static WayangA2uiHttpReadinessProbeResult fromMap(Map<?, ?> values) {
        Map<String, Object> copy = TransportMaps.copy(values);
        if (copy.isEmpty()) {
            return WayangA2uiHttpReadinessProbeResult.empty();
        }
        return new WayangA2uiHttpReadinessProbeResult(
                WayangA2uiHttpBindingReportProbeResult.fromMap(
                        TransportMaps.copyMap(copy.get("bindingReportProbe"))),
                actionBindingProbe(copy),
                WayangA2uiHttpSmokeProbeResult.fromMap(TransportMaps.copyMap(copy.get("smokeProbe"))),
                DecodeValues.bool(copy.get("smokeRequired"), false));
    }

    public static WayangA2uiHttpReadinessProbeResult fromJson(String json) {
        return fromMap(TransportJson.map(
                json,
                "A2UI HTTP readiness probe result JSON must not be blank",
                "Unable to decode A2UI HTTP readiness probe result JSON"));
    }

    private WayangA2uiHttpReadinessProbeResultDecoder() {
    }

    private static WayangA2uiHttpActionBindingProbeResult actionBindingProbe(Map<String, Object> copy) {
        if (!copy.containsKey("actionBindingProbe")) {
            return WayangA2uiHttpActionBindingProbeResult.compatibilityFallback();
        }
        return WayangA2uiHttpActionBindingProbeResult.fromMap(
                TransportMaps.copyMap(copy.get("actionBindingProbe")));
    }
}

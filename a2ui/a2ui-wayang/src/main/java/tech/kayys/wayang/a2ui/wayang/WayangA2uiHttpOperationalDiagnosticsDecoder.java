package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.wayang.transport.TransportJson;
import tech.kayys.wayang.a2ui.wayang.transport.TransportMaps;

import java.util.Map;

/**
 * Canonical decoder for stored or remote A2UI HTTP operational diagnostics.
 */
public final class WayangA2uiHttpOperationalDiagnosticsDecoder {

    public static WayangA2uiHttpOperationalDiagnostics fromMap(Map<?, ?> values) {
        Map<String, Object> copy = TransportMaps.copy(values);
        if (copy.isEmpty()) {
            return WayangA2uiHttpOperationalDiagnostics.empty();
        }
        Map<String, Object> readinessProbe = TransportMaps.copyMap(copy.get("readinessProbe"));
        if (readinessProbe.isEmpty()) {
            readinessProbe = copy;
        }
        return new WayangA2uiHttpOperationalDiagnostics(
                WayangA2uiHttpReadinessProbeResult.fromMap(readinessProbe));
    }

    public static WayangA2uiHttpOperationalDiagnostics fromJson(String json) {
        return fromMap(TransportJson.map(
                json,
                "A2UI HTTP operational diagnostics JSON must not be blank",
                "Unable to decode A2UI HTTP operational diagnostics JSON"));
    }

    private WayangA2uiHttpOperationalDiagnosticsDecoder() {
    }
}

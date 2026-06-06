package tech.kayys.wayang.a2ui.wayang;

import java.util.Map;

/**
 * Canonical decoder for stored or remote A2UI HTTP smoke probe results.
 */
public final class WayangA2uiHttpSmokeProbeResultDecoder {

    public static WayangA2uiHttpSmokeProbeResult fromMap(Map<?, ?> values) {
        Map<String, Object> copy = WayangA2uiTransportMaps.copy(values);
        return new WayangA2uiHttpSmokeProbeResult(
                WayangA2uiDecodeValues.nonNegativeInt(copy.get("statusCode"), 0),
                WayangA2uiDecodeValues.bool(copy.get("httpSuccessful"), false),
                WayangA2uiDecodeValues.text(copy.get("routeOperation")),
                WayangA2uiDecodeValues.text(copy.get("allow")),
                WayangA2uiDecodeValues.text(copy.get(WayangA2uiTransportFields.OUTCOME)),
                WayangA2uiHttpSmokeSummary.fromMap(WayangA2uiTransportMaps.copyMap(copy.get("summary"))),
                WayangA2uiTransportMaps.copyMap(copy.get("headers")));
    }

    public static WayangA2uiHttpSmokeProbeResult fromJson(String json) {
        return fromMap(WayangA2uiTransportJson.map(
                json,
                "A2UI HTTP smoke probe result JSON must not be blank",
                "Unable to decode A2UI HTTP smoke probe result JSON"));
    }

    private WayangA2uiHttpSmokeProbeResultDecoder() {
    }
}

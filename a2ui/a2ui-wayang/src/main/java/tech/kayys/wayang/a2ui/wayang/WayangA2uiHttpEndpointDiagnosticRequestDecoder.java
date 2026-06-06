package tech.kayys.wayang.a2ui.wayang;

import java.util.Map;

/**
 * Canonical decoder for stored or remote A2UI HTTP endpoint diagnostic requests.
 */
public final class WayangA2uiHttpEndpointDiagnosticRequestDecoder {

    public static WayangA2uiHttpEndpointDiagnosticRequest fromMap(Map<?, ?> values) {
        if (values == null || values.isEmpty()) {
            return WayangA2uiHttpEndpointDiagnosticRequest.get("/");
        }
        Map<String, Object> request = WayangA2uiTransportMaps.copy(values);
        return WayangA2uiHttpEndpointDiagnosticRequest.of(
                WayangA2uiDecodeValues.text(request.get(WayangA2uiHttpEndpointDiagnosticRequest.KEY_METHOD)),
                path(request),
                WayangA2uiDecodeValues.text(request.get(WayangA2uiHttpEndpointDiagnosticRequest.KEY_BODY)),
                WayangA2uiTransportMaps.copyMap(
                        request.get(WayangA2uiHttpEndpointDiagnosticRequest.KEY_HEADERS)),
                WayangA2uiTransportMaps.copyMap(
                        request.get(WayangA2uiHttpEndpointDiagnosticRequest.KEY_ATTRIBUTES)));
    }

    public static WayangA2uiHttpEndpointDiagnosticRequest fromJson(String json) {
        return fromMap(WayangA2uiTransportJson.map(
                json,
                "A2UI HTTP endpoint diagnostic request JSON must not be blank",
                "Unable to decode A2UI HTTP endpoint diagnostic request JSON"));
    }

    private static String path(Map<String, Object> values) {
        String rawPath = WayangA2uiDecodeValues.text(
                values.get(WayangA2uiHttpEndpointDiagnosticRequest.KEY_RAW_PATH));
        return rawPath.isBlank()
                ? WayangA2uiDecodeValues.text(values.get(WayangA2uiHttpEndpointDiagnosticRequest.KEY_PATH))
                : rawPath;
    }

    private WayangA2uiHttpEndpointDiagnosticRequestDecoder() {
    }
}

package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.wayang.transport.TransportJson;
import tech.kayys.wayang.a2ui.wayang.transport.TransportMaps;

import tech.kayys.wayang.a2ui.wayang.support.DecodeValues;

import java.util.Map;

/**
 * Canonical decoder for stored or remote A2UI HTTP endpoint diagnostic requests.
 */
public final class WayangA2uiHttpEndpointDiagnosticRequestDecoder {

    public static WayangA2uiHttpEndpointDiagnosticRequest fromMap(Map<?, ?> values) {
        if (values == null || values.isEmpty()) {
            return WayangA2uiHttpEndpointDiagnosticRequest.defaultRequest();
        }
        Map<String, Object> request = TransportMaps.copy(values);
        return WayangA2uiHttpEndpointDiagnosticRequest.of(
                DecodeValues.text(request.get(WayangA2uiHttpEndpointDiagnosticRequest.KEY_METHOD)),
                path(request),
                DecodeValues.rawText(request.get(WayangA2uiHttpEndpointDiagnosticRequest.KEY_BODY)),
                TransportMaps.copyMap(
                        request.get(WayangA2uiHttpEndpointDiagnosticRequest.KEY_HEADERS)),
                TransportMaps.copyMap(
                        request.get(WayangA2uiHttpEndpointDiagnosticRequest.KEY_ATTRIBUTES)));
    }

    public static WayangA2uiHttpEndpointDiagnosticRequest fromJson(String json) {
        return fromMap(TransportJson.map(
                json,
                "A2UI HTTP endpoint diagnostic request JSON must not be blank",
                "Unable to decode A2UI HTTP endpoint diagnostic request JSON"));
    }

    private static String path(Map<String, Object> values) {
        String rawPath = DecodeValues.text(
                values.get(WayangA2uiHttpEndpointDiagnosticRequest.KEY_RAW_PATH));
        return rawPath.isBlank()
                ? DecodeValues.text(values.get(WayangA2uiHttpEndpointDiagnosticRequest.KEY_PATH))
                : rawPath;
    }

    private WayangA2uiHttpEndpointDiagnosticRequestDecoder() {
    }
}

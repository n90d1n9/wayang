package tech.kayys.wayang.a2ui.wayang.http;

import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpResponse;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpRoute;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiTransportContent;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiTransportOutcome;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiTransportResponse;
import tech.kayys.wayang.a2ui.wayang.transport.TransportJson;

import java.util.List;
import java.util.Map;

/**
 * Small shared fixtures for HTTP response decoder tests.
 */
public final class HttpResponseDecoderTestFixtures {

    public static WayangA2uiHttpResponse jsonResponse(String body) {
        return new WayangA2uiHttpResponse(200, WayangA2uiTransportContent.MIME_JSON, body, Map.of());
    }

    public static WayangA2uiHttpResponse jsonResponse(String body, WayangA2uiHttpRoute route) {
        return new WayangA2uiHttpResponse(200, WayangA2uiTransportContent.MIME_JSON, body, Map.of(
                WayangA2uiHttpResponse.HEADER_A2UI_ROUTE_OPERATION,
                route.operation(),
                WayangA2uiHttpResponse.HEADER_ALLOW,
                route.allowHeader(),
                WayangA2uiHttpResponse.HEADER_A2UI_OUTCOME,
                WayangA2uiTransportOutcome.SUCCESS.name()));
    }

    public static String jsonBody(Map<String, Object> body) {
        return TransportJson.json(body, "Unable to encode A2UI response decoder test body");
    }

    public static String transportEnvelopeJson(Map<String, Object> body, Map<String, Object> metadata) {
        return new WayangA2uiTransportResponse(
                WayangA2uiTransportContent.MIME_JSON,
                WayangA2uiTransportContent.ENCODING_JSON,
                jsonBody(body),
                List.of(),
                1,
                0,
                metadata)
                .toJson();
    }

    private HttpResponseDecoderTestFixtures() {
    }
}

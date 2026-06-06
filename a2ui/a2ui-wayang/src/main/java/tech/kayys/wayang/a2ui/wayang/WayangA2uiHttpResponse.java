package tech.kayys.wayang.a2ui.wayang;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Dependency-free HTTP-shaped response for A2UI bridge adapters.
 */
public record WayangA2uiHttpResponse(
        int statusCode,
        String contentType,
        String body,
        Map<String, Object> headers) {

    public static final String HEADER_CONTENT_TYPE = "Content-Type";
    public static final String HEADER_ACCEPT = "Accept";
    public static final String HEADER_A2UI_MIME_TYPE = "X-Wayang-A2UI-Mime-Type";
    public static final String HEADER_A2UI_BODY_ENCODING = "X-Wayang-A2UI-Body-Encoding";
    public static final String HEADER_A2UI_OUTCOME = "X-Wayang-A2UI-Outcome";
    public static final String HEADER_A2UI_ROUTE_OPERATION = "X-Wayang-A2UI-Route-Operation";
    public static final String HEADER_ALLOW = "Allow";

    public WayangA2uiHttpResponse {
        statusCode = statusCode <= 0 ? 200 : statusCode;
        contentType = contentType == null || contentType.isBlank()
                ? WayangA2uiTransportContent.MIME_JSON
                : contentType.trim();
        body = body == null ? "" : body;
        headers = WayangA2uiTransportMaps.copy(headers);
    }

    public static WayangA2uiHttpResponse fromBridge(WayangA2uiBridgeResponse response) {
        return fromBridge(defaultStatus(response), response);
    }

    public static WayangA2uiHttpResponse fromBridge(int statusCode, WayangA2uiBridgeResponse response) {
        WayangA2uiBridgeResponse resolved = Objects.requireNonNull(response, "response");
        WayangA2uiTransportResponse transport = resolved.transportResponse();
        Map<String, Object> headers = new LinkedHashMap<>();
        headers.put(HEADER_CONTENT_TYPE, WayangA2uiTransportContent.MIME_JSON);
        headers.put(HEADER_A2UI_MIME_TYPE, transport.mimeType());
        headers.put(HEADER_A2UI_BODY_ENCODING, transport.bodyEncoding());
        headers.put(HEADER_A2UI_OUTCOME, transport.outcome().name());
        return new WayangA2uiHttpResponse(
                statusCode,
                WayangA2uiTransportContent.MIME_JSON,
                resolved.transportEnvelopeJson(),
                headers);
    }

    public static WayangA2uiHttpResponse error(int statusCode, String code, String message) {
        return fromBridge(statusCode, WayangA2uiBridgeResponse.of(WayangA2uiTransportResponse.error(code, message)));
    }

    public boolean successful() {
        return statusCode >= 200 && statusCode < 300;
    }

    public WayangA2uiHttpResponse withHeaders(Map<?, ?> extraHeaders) {
        if (extraHeaders == null || extraHeaders.isEmpty()) {
            return this;
        }
        Map<String, Object> merged = new LinkedHashMap<>(headers);
        merged.putAll(WayangA2uiTransportMaps.copy(extraHeaders));
        return new WayangA2uiHttpResponse(statusCode, contentType, body, merged);
    }

    public WayangA2uiHttpResponse withRoute(WayangA2uiHttpRoute route) {
        WayangA2uiHttpRoute resolved = Objects.requireNonNull(route, "route");
        return withHeaders(Map.of(
                HEADER_ALLOW, resolved.allowHeader(),
                HEADER_A2UI_ROUTE_OPERATION, resolved.operation()));
    }

    private static int defaultStatus(WayangA2uiBridgeResponse response) {
        WayangA2uiBridgeResponse resolved = Objects.requireNonNull(response, "response");
        return resolved.transportError().isPresent() ? 400 : 200;
    }
}

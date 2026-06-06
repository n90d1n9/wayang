package tech.kayys.wayang.a2ui.wayang;

import java.util.Map;

/**
 * Raw HTTP call shape for mounted A2UI endpoint diagnostics.
 */
public record WayangA2uiHttpEndpointDiagnosticRequest(
        String method,
        String rawPath,
        String body,
        Map<String, Object> headers,
        Map<String, Object> attributes) {

    public static final String KEY_METHOD = "method";
    public static final String KEY_RAW_PATH = "rawPath";
    public static final String KEY_PATH = "path";
    public static final String KEY_BODY = "body";
    public static final String KEY_HEADERS = "headers";
    public static final String KEY_ATTRIBUTES = "attributes";

    public WayangA2uiHttpEndpointDiagnosticRequest {
        method = WayangA2uiHttpRequest.normalizeMethod(method);
        rawPath = normalizeRawPath(rawPath);
        body = body == null ? "" : body;
        headers = WayangA2uiTransportMaps.copy(headers);
        attributes = WayangA2uiTransportMaps.copy(attributes);
    }

    public static WayangA2uiHttpEndpointDiagnosticRequest of(String method, String rawPath) {
        return of(method, rawPath, "", Map.of(), Map.of());
    }

    public static WayangA2uiHttpEndpointDiagnosticRequest of(
            String method,
            String rawPath,
            String body,
            Map<?, ?> headers,
            Map<?, ?> attributes) {
        return new WayangA2uiHttpEndpointDiagnosticRequest(
                method,
                rawPath,
                body,
                WayangA2uiTransportMaps.copy(headers),
                WayangA2uiTransportMaps.copy(attributes));
    }

    public static WayangA2uiHttpEndpointDiagnosticRequest fromMap(Map<?, ?> values) {
        return WayangA2uiHttpEndpointDiagnosticRequestDecoder.fromMap(values);
    }

    public static WayangA2uiHttpEndpointDiagnosticRequest fromJson(String json) {
        return WayangA2uiHttpEndpointDiagnosticRequestDecoder.fromJson(json);
    }

    public static WayangA2uiHttpEndpointDiagnosticRequest get(String rawPath) {
        return of("GET", rawPath);
    }

    public static WayangA2uiHttpEndpointDiagnosticRequest postJson(String rawPath, String body) {
        return of(
                "POST",
                rawPath,
                body,
                Map.of(WayangA2uiHttpResponse.HEADER_CONTENT_TYPE, WayangA2uiTransportContent.MIME_JSON),
                Map.of());
    }

    public WayangA2uiHttpEndpointDiagnosticRequest withHeaders(Map<?, ?> extraHeaders) {
        if (extraHeaders == null || extraHeaders.isEmpty()) {
            return this;
        }
        return new WayangA2uiHttpEndpointDiagnosticRequest(
                method,
                rawPath,
                body,
                WayangA2uiTransportMetadata.merge(headers, WayangA2uiTransportMaps.copy(extraHeaders)),
                attributes);
    }

    public WayangA2uiHttpEndpointDiagnosticRequest withAttributes(Map<?, ?> extraAttributes) {
        if (extraAttributes == null || extraAttributes.isEmpty()) {
            return this;
        }
        return new WayangA2uiHttpEndpointDiagnosticRequest(
                method,
                rawPath,
                body,
                headers,
                WayangA2uiTransportMetadata.merge(attributes, WayangA2uiTransportMaps.copy(extraAttributes)));
    }

    public Map<String, Object> toMap() {
        return WayangA2uiHttpEndpointDiagnosticPlanProjection.request(this);
    }

    public String toJson() {
        return WayangA2uiTransportJson.json(toMap(), "Unable to encode A2UI HTTP endpoint diagnostic request");
    }

    private static String normalizeRawPath(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isBlank()) {
            return "/";
        }
        return normalized.startsWith("/") ? normalized : "/" + normalized;
    }
}

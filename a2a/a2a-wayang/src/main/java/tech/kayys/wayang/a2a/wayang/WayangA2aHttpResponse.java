package tech.kayys.wayang.a2a.wayang;

import tech.kayys.wayang.a2a.core.A2aAgentCard;
import tech.kayys.wayang.a2a.core.A2aHttpRoute;
import tech.kayys.wayang.a2a.core.A2aProtocol;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Dependency-free HTTP-shaped response for A2A bindings.
 */
public record WayangA2aHttpResponse(
        int statusCode,
        String contentType,
        String body,
        Map<String, Object> headers) {

    public static final String HEADER_CONTENT_TYPE = "Content-Type";
    public static final String HEADER_ACCEPT = "Accept";
    public static final String HEADER_ALLOW = "Allow";
    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String HEADER_CACHE_CONTROL = "Cache-Control";
    public static final String HEADER_ETAG = "ETag";
    public static final String HEADER_IF_NONE_MATCH = "If-None-Match";
    public static final String HEADER_WWW_AUTHENTICATE = "WWW-Authenticate";
    public static final String HEADER_A2A_EXTENSIONS = A2aProtocol.HEADER_EXTENSIONS;
    public static final String HEADER_A2A_VERSION = A2aProtocol.HEADER_VERSION;
    public static final String HEADER_A2A_ROUTE_OPERATION = "X-Wayang-A2A-Route-Operation";
    public static final String HEADER_A2A_PROTOCOL_VERSION = "X-Wayang-A2A-Protocol-Version";
    public static final String HEADER_A2A_STREAMING = "X-Wayang-A2A-Streaming";
    public static final String AGENT_CARD_CACHE_CONTROL = "public, max-age=300";

    public WayangA2aHttpResponse {
        statusCode = statusCode <= 0 ? 200 : statusCode;
        contentType = contentType == null || contentType.isBlank()
                ? A2aProtocol.MEDIA_TYPE
                : contentType.trim();
        body = body == null ? "" : body;
        headers = WayangA2aMaps.copyMap(headers);
    }

    public static WayangA2aHttpResponse json(int statusCode, String body) {
        return new WayangA2aHttpResponse(
                statusCode,
                A2aProtocol.MEDIA_TYPE,
                body,
                Map.of(HEADER_CONTENT_TYPE, A2aProtocol.MEDIA_TYPE));
    }

    public static WayangA2aHttpResponse object(int statusCode, Map<String, Object> payload) {
        return json(statusCode, WayangA2aHttpJson.write(WayangA2aMaps.copyMap(payload)));
    }

    public static WayangA2aHttpResponse agentCard(A2aAgentCard card) {
        String body = Objects.requireNonNull(card, "card").toJson();
        return new WayangA2aHttpResponse(
                200,
                A2aProtocol.MEDIA_TYPE,
                body,
                agentCardHeaders(body, true));
    }

    public static WayangA2aHttpResponse agentCardNotModified(A2aAgentCard card) {
        String body = Objects.requireNonNull(card, "card").toJson();
        return new WayangA2aHttpResponse(
                304,
                A2aProtocol.MEDIA_TYPE,
                "",
                agentCardHeaders(body, false));
    }

    public static String agentCardEtag(A2aAgentCard card) {
        return etag(Objects.requireNonNull(card, "card").toJson());
    }

    public static WayangA2aHttpResponse eventStream(int statusCode, String body) {
        return new WayangA2aHttpResponse(
                statusCode,
                A2aProtocol.EVENT_STREAM_MEDIA_TYPE,
                body,
                Map.of(HEADER_CONTENT_TYPE, A2aProtocol.EVENT_STREAM_MEDIA_TYPE));
    }

    public static WayangA2aHttpResponse error(int statusCode, String code, String message) {
        return error(statusCode, code, message, Map.of());
    }

    public static WayangA2aHttpResponse error(
            int statusCode,
            String code,
            String message,
            Map<String, Object> metadata) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("code", WayangA2aMaps.required(code, "code"));
        error.put("message", WayangA2aMaps.required(message, "message"));
        if (metadata != null && !metadata.isEmpty()) {
            error.put("metadata", WayangA2aMaps.copyMap(metadata));
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("error", WayangA2aMaps.copyMap(error));
        return object(statusCode, payload);
    }

    public boolean successful() {
        return statusCode >= 200 && statusCode < 300;
    }

    public WayangA2aHttpResponse withHeaders(Map<?, ?> extraHeaders) {
        if (extraHeaders == null || extraHeaders.isEmpty()) {
            return this;
        }
        Map<String, Object> merged = new LinkedHashMap<>(headers);
        merged.putAll(WayangA2aMaps.copyMap(extraHeaders));
        return new WayangA2aHttpResponse(statusCode, contentType, body, merged);
    }

    public WayangA2aHttpResponse withRoute(A2aHttpRoute route) {
        A2aHttpRoute resolved = Objects.requireNonNull(route, "route");
        Map<String, Object> headers = new LinkedHashMap<>(protocolHeaders(resolved.operation()));
        headers.put(HEADER_A2A_STREAMING, resolved.streaming());
        return withHeaders(WayangA2aMaps.copyMap(headers));
    }

    public static Map<String, Object> protocolHeaders(String operation) {
        return protocolHeaders(operation, A2aProtocol.VERSION);
    }

    public static Map<String, Object> protocolHeaders(String operation, String protocolVersion) {
        String resolvedVersion = protocolVersion == null || protocolVersion.isBlank()
                ? A2aProtocol.VERSION
                : protocolVersion.trim();
        Map<String, Object> headers = new LinkedHashMap<>();
        headers.put(HEADER_A2A_ROUTE_OPERATION, Objects.requireNonNull(operation, "operation"));
        headers.put(HEADER_A2A_PROTOCOL_VERSION, resolvedVersion);
        headers.put(HEADER_A2A_VERSION, resolvedVersion);
        return WayangA2aMaps.copyMap(headers);
    }

    private static Map<String, Object> agentCardHeaders(String body, boolean includeContentType) {
        Map<String, Object> headers = new LinkedHashMap<>();
        if (includeContentType) {
            headers.put(HEADER_CONTENT_TYPE, A2aProtocol.MEDIA_TYPE);
        }
        headers.put(HEADER_CACHE_CONTROL, AGENT_CARD_CACHE_CONTROL);
        headers.put(HEADER_ETAG, etag(body));
        return WayangA2aMaps.copyMap(headers);
    }

    private static String etag(String body) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(body.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                hex.append(String.format("%02x", value));
            }
            return "\"" + hex + "\"";
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 digest is not available", e);
        }
    }
}

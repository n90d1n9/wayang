package tech.kayys.wayang.a2a.wayang;

import tech.kayys.wayang.a2a.core.A2aProtocol;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Shared JSON-RPC HTTP response construction.
 */
final class WayangA2aJsonRpcHttpResponses {

    private WayangA2aJsonRpcHttpResponses() {
    }

    static WayangA2aHttpResponse jsonRpc(WayangA2aJsonRpcResponse response) {
        return jsonBody(200, Objects.requireNonNull(response, "response").toJson());
    }

    static WayangA2aHttpResponse jsonRpcResult(Object id, Map<String, Object> result) {
        return jsonRpc(WayangA2aJsonRpcResponse.result(id, result));
    }

    static WayangA2aHttpResponse eventStream(String body) {
        return WayangA2aHttpResponse.eventStream(200, body);
    }

    static WayangA2aHttpResponse json(String operation, String body) {
        return json(200, operation, body, null);
    }

    static WayangA2aHttpResponse jsonWithProtocolVersion(String operation, String protocolVersion, String body) {
        return jsonBody(200, body, headersWithProtocolVersion(operation, protocolVersion, null));
    }

    static WayangA2aHttpResponse json(int statusCode, String operation, String body, String allow) {
        return jsonBody(statusCode, body, headers(operation, allow));
    }

    static WayangA2aHttpResponse error(
            int statusCode,
            String operation,
            String allow,
            String code,
            String message) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("code", WayangA2aMaps.required(code, "code"));
        error.put("message", WayangA2aMaps.required(message, "message"));
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("error", WayangA2aMaps.copyMap(error));
        return json(
                statusCode,
                operation,
                WayangA2aHttpJson.write(WayangA2aMaps.copyMap(payload)),
                allow);
    }

    static WayangA2aHttpResponse jsonRpcError(
            int statusCode,
            String operation,
            String allow,
            Object id,
            WayangA2aJsonRpcError error) {
        return json(
                statusCode,
                operation,
                WayangA2aJsonRpcResponse.error(id, Objects.requireNonNull(error, "error")).toJson(),
                allow);
    }

    static Map<String, Object> headers(String operation) {
        return headers(operation, null);
    }

    static Map<String, Object> headers(String operation, String allow) {
        return headersWithProtocolVersion(operation, A2aProtocol.VERSION, allow);
    }

    static Map<String, Object> headersWithProtocolVersion(String operation, String protocolVersion, String allow) {
        Map<String, Object> headers = new LinkedHashMap<>();
        headers.put(WayangA2aHttpResponse.HEADER_CONTENT_TYPE, WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON);
        headers.putAll(routeHeadersWithProtocolVersion(operation, protocolVersion, allow));
        return WayangA2aMaps.copyMap(headers);
    }

    static Map<String, Object> routeHeaders(String operation, String allow) {
        return routeHeadersWithProtocolVersion(operation, A2aProtocol.VERSION, allow);
    }

    static Map<String, Object> routeHeadersWithProtocolVersion(
            String operation,
            String protocolVersion,
            String allow) {
        Map<String, Object> headers = new LinkedHashMap<>(
                WayangA2aHttpResponse.protocolHeaders(operation, protocolVersion));
        if (allow != null && !allow.isBlank()) {
            headers.put(WayangA2aHttpResponse.HEADER_ALLOW, allow.trim());
        }
        return WayangA2aMaps.copyMap(headers);
    }

    private static WayangA2aHttpResponse jsonBody(int statusCode, String body) {
        return jsonBody(statusCode, body, Map.of(
                WayangA2aHttpResponse.HEADER_CONTENT_TYPE,
                WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON));
    }

    private static WayangA2aHttpResponse jsonBody(int statusCode, String body, Map<String, Object> headers) {
        return new WayangA2aHttpResponse(
                statusCode,
                WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON,
                body,
                headers);
    }
}

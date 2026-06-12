package tech.kayys.wayang.a2a.wayang;

import tech.kayys.wayang.a2a.core.A2aProtocol;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Ordered projection helpers for A2A JSON-RPC HTTP route descriptors.
 */
final class WayangA2aJsonRpcHttpRouteProjection {

    private WayangA2aJsonRpcHttpRouteProjection() {
    }

    static Map<String, Object> route(WayangA2aJsonRpcHttpRoute route) {
        WayangA2aJsonRpcHttpRoute resolved = Objects.requireNonNull(route, "route");
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("operation", resolved.operation());
        values.put("enabled", resolved.enabled());
        values.put("path", resolved.path());
        values.put("httpMethod", resolved.httpMethod());
        values.put("allowedMethods", resolved.allowedMethods());
        values.put("allow", resolved.allow());
        values.put("requestMediaType", resolved.requestMediaType());
        values.put("responseMediaTypes", resolved.responseMediaTypes());
        values.put("requestBodyRequired", resolved.requestBodyRequired());
        return WayangA2aMaps.copyMap(values);
    }

    static Map<String, Object> bindingReportRoute(WayangA2aJsonRpcHttpRouteDescriptor route) {
        WayangA2aJsonRpcHttpRouteDescriptor resolved = Objects.requireNonNull(route, "route");
        Map<String, Object> values = new LinkedHashMap<>();
        if (!endpoint(resolved)) {
            values.put("enabled", resolved.enabled());
        }
        values.put("path", resolved.path());
        values.put("httpMethod", resolved.httpMethod());
        values.put("allow", resolved.allow());
        if (!resolved.requestMediaType().isBlank()) {
            values.put("requestMediaType", resolved.requestMediaType());
        }
        if (endpoint(resolved)) {
            values.put("responseMediaTypes", resolved.responseMediaTypes());
        } else {
            values.put("responseMediaType", responseMediaType(resolved));
        }
        return WayangA2aMaps.copyMap(values);
    }

    static WayangA2aHttpResponse optionsResponse(WayangA2aJsonRpcHttpRouteDescriptor route) {
        WayangA2aJsonRpcHttpRouteDescriptor resolved = Objects.requireNonNull(route, "route");
        return WayangA2aJsonRpcHttpResponses.json(
                200,
                resolved.operation(),
                WayangA2aHttpJson.write(optionsPayload(resolved)),
                resolved.allow());
    }

    static Map<String, Object> optionsPayload(WayangA2aJsonRpcHttpRouteDescriptor route) {
        WayangA2aJsonRpcHttpRouteDescriptor resolved = Objects.requireNonNull(route, "route");
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("binding", A2aProtocol.BINDING_JSONRPC);
        payload.put("operation", resolved.operation());
        payload.put("path", resolved.path());
        payload.put("allow", resolved.allow());
        payload.put("protocolVersion", A2aProtocol.VERSION);
        return WayangA2aMaps.copyMap(payload);
    }

    private static boolean endpoint(WayangA2aJsonRpcHttpRouteDescriptor route) {
        return WayangA2aJsonRpcHttpRouteDescriptor.KEY_ENDPOINT.equals(route.key());
    }

    private static String responseMediaType(WayangA2aJsonRpcHttpRouteDescriptor route) {
        return route.responseMediaTypes().isEmpty() ? "" : route.responseMediaTypes().getFirst();
    }
}

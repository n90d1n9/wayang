package tech.kayys.wayang.a2a.wayang;

import java.util.List;
import java.util.Map;

/**
 * Framework-neutral descriptor for one A2A JSON-RPC HTTP binding route.
 */
public record WayangA2aJsonRpcHttpRoute(
        String operation,
        boolean enabled,
        String path,
        String httpMethod,
        String requestMediaType,
        List<String> responseMediaTypes,
        boolean requestBodyRequired) {

    public WayangA2aJsonRpcHttpRoute {
        operation = WayangA2aMaps.required(operation, "operation");
        path = WayangA2aHttpRequest.normalizePath(path);
        httpMethod = WayangA2aHttpRequest.normalizeMethod(httpMethod);
        requestMediaType = requestMediaType == null ? "" : requestMediaType.trim();
        responseMediaTypes = responseMediaTypes == null || responseMediaTypes.isEmpty()
                ? List.of(WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON)
                : responseMediaTypes.stream()
                        .map(value -> value == null ? "" : value.trim())
                        .filter(value -> !value.isBlank())
                        .distinct()
                        .toList();
    }

    public boolean operation(String expectedOperation) {
        String normalized = expectedOperation == null ? "" : expectedOperation.trim();
        return operation.equals(normalized);
    }

    public boolean matches(WayangA2aHttpRequest request) {
        return request != null && request.method(httpMethod) && request.path().equals(path);
    }

    public List<String> allowedMethods() {
        return List.of(httpMethod, "OPTIONS");
    }

    public String allow() {
        return String.join(", ", allowedMethods());
    }

    public String responseMediaType() {
        return responseMediaTypes.isEmpty() ? "" : responseMediaTypes.getFirst();
    }

    public Map<String, Object> toMap() {
        return WayangA2aJsonRpcHttpRouteProjection.route(this);
    }
}

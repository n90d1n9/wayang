package tech.kayys.wayang.a2a.wayang;

import tech.kayys.wayang.a2a.core.A2aHttpRoute;
import tech.kayys.wayang.a2a.core.A2aProtocol;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One A2A v1.0 operation mapping across JSON-RPC, gRPC, and HTTP+JSON bindings.
 */
public record WayangA2aJsonRpcSpecOperation(
        String operation,
        String jsonRpcMethod,
        String grpcMethod,
        String restMethod,
        String restPath,
        boolean streaming,
        String requestMediaType,
        String responseMediaType,
        boolean supported,
        String description) {

    public WayangA2aJsonRpcSpecOperation {
        operation = WayangA2aMaps.required(operation, "operation");
        jsonRpcMethod = WayangA2aMaps.required(jsonRpcMethod, "jsonRpcMethod");
        grpcMethod = grpcMethod == null ? "" : grpcMethod.trim();
        restMethod = WayangA2aHttpRequest.normalizeMethod(restMethod);
        restPath = WayangA2aHttpRequest.normalizePath(restPath);
        requestMediaType = requestMediaType == null || requestMediaType.isBlank()
                ? WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON
                : requestMediaType.trim();
        responseMediaType = responseMediaType == null || responseMediaType.isBlank()
                ? defaultResponseMediaType(streaming)
                : responseMediaType.trim();
        description = description == null ? "" : description.trim();
    }

    public static List<WayangA2aJsonRpcSpecOperation> standardOperations() {
        return WayangA2aJsonRpcMethods.descriptors().stream()
                .map(WayangA2aJsonRpcSpecOperation::fromDescriptor)
                .toList();
    }

    public static WayangA2aJsonRpcSpecOperation fromDescriptor(WayangA2aJsonRpcMethods.Descriptor descriptor) {
        WayangA2aJsonRpcMethods.Descriptor resolved = java.util.Objects.requireNonNull(descriptor, "descriptor");
        return new WayangA2aJsonRpcSpecOperation(
                resolved.operation(),
                resolved.method(),
                resolved.grpcMethod(),
                resolved.restMethod(),
                resolved.restPath(),
                resolved.streaming(),
                resolved.requestMediaType(),
                resolved.responseMediaType(),
                true,
                resolved.description());
    }

    public static WayangA2aJsonRpcSpecOperation fromRoute(A2aHttpRoute route) {
        A2aHttpRoute resolved = java.util.Objects.requireNonNull(route, "route");
        String jsonRpcMethod = resolved.jsonRpcMethod();
        boolean supported = WayangA2aJsonRpcMethods.descriptor(jsonRpcMethod)
                .map(WayangA2aJsonRpcMethods.Descriptor::operation)
                .filter(resolved.operation()::equals)
                .isPresent();
        return new WayangA2aJsonRpcSpecOperation(
                resolved.operation(),
                jsonRpcMethod,
                resolved.grpcMethod(),
                resolved.httpMethod(),
                resolved.path(),
                resolved.streaming(),
                WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON,
                defaultResponseMediaType(resolved.streaming()),
                supported,
                resolved.description());
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("operation", operation);
        values.put("jsonRpcMethod", jsonRpcMethod);
        values.put("grpcMethod", grpcMethod);
        values.put("restMethod", restMethod);
        values.put("restPath", restPath);
        values.put("streaming", streaming);
        values.put("requestMediaType", requestMediaType);
        values.put("responseMediaType", responseMediaType);
        values.put("supported", supported);
        values.put("description", description);
        return WayangA2aMaps.copyMap(values);
    }

    private static String defaultResponseMediaType(boolean streaming) {
        return streaming
                ? A2aProtocol.EVENT_STREAM_MEDIA_TYPE
                : WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON;
    }
}

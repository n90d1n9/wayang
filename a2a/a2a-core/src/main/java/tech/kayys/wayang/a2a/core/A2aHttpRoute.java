package tech.kayys.wayang.a2a.core;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * REST binding route metadata for an A2A operation.
 */
public record A2aHttpRoute(
        String operation,
        String jsonRpcMethod,
        String grpcMethod,
        String httpMethod,
        String path,
        boolean streaming,
        String description) {

    public A2aHttpRoute {
        operation = A2aValues.required(operation, "operation");
        jsonRpcMethod = A2aValues.optional(jsonRpcMethod);
        grpcMethod = A2aValues.optional(grpcMethod);
        httpMethod = A2aValues.required(httpMethod, "httpMethod").toUpperCase();
        path = A2aValues.required(path, "path");
        description = A2aValues.optional(description);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("operation", operation);
        A2aValues.putOptional(payload, "jsonRpcMethod", jsonRpcMethod);
        A2aValues.putOptional(payload, "grpcMethod", grpcMethod);
        payload.put("httpMethod", httpMethod);
        payload.put("path", path);
        payload.put("streaming", streaming);
        A2aValues.putOptional(payload, "description", description);
        return A2aValues.copyMap(payload);
    }
}

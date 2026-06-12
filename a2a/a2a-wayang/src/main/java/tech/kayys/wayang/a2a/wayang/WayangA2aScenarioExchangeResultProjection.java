package tech.kayys.wayang.a2a.wayang;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Shared ordered projection for one A2A scenario exchange result.
 */
final class WayangA2aScenarioExchangeResultProjection {

    private WayangA2aScenarioExchangeResultProjection() {
    }

    static Map<String, Object> exchange(
            int index,
            Object requestId,
            String method,
            String path,
            int statusCode,
            boolean successful,
            String contentType,
            String operation,
            Map<String, Object> error,
            Map<String, Object> headers,
            Object body) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("index", index);
        if (requestId != null) {
            values.put("requestId", requestId);
        }
        values.put("method", WayangA2aMaps.required(method, "method"));
        String normalizedPath = WayangA2aMaps.optional(path);
        if (normalizedPath != null) {
            values.put("path", normalizedPath);
        }
        values.put("statusCode", statusCode);
        values.put("successful", successful);
        values.put("contentType", contentType == null ? "" : contentType);
        String normalizedOperation = WayangA2aMaps.optional(operation);
        if (normalizedOperation != null) {
            values.put("operation", normalizedOperation);
        }
        if (error != null && !error.isEmpty()) {
            values.put("error", error);
        }
        values.put("headers", WayangA2aMaps.copyMap(headers));
        values.put("body", body);
        return WayangA2aMaps.copyMap(values);
    }
}

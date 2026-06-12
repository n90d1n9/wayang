package tech.kayys.wayang.a2a.wayang;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Shared ordered projection for one A2A scenario exchange definition.
 */
final class WayangA2aScenarioExchangeDefinitionProjection {

    private WayangA2aScenarioExchangeDefinitionProjection() {
    }

    static Map<String, Object> exchange(
            Object requestId,
            String method,
            String path,
            Map<String, Object> headers,
            Map<String, Object> requestAttributes,
            Map<String, Object> params,
            Map<String, Object> attributes) {
        Map<String, Object> values = new LinkedHashMap<>();
        if (requestId != null) {
            values.put("requestId", requestId);
        }
        values.put("method", WayangA2aMaps.required(method, "method"));
        String normalizedPath = WayangA2aMaps.optional(path);
        if (normalizedPath != null) {
            values.put("path", normalizedPath);
        }
        if (headers != null) {
            values.put("headers", WayangA2aMaps.copyMap(headers));
        }
        if (requestAttributes != null && !requestAttributes.isEmpty()) {
            values.put("requestAttributes", WayangA2aMaps.copyMap(requestAttributes));
        }
        if (params != null) {
            values.put("params", WayangA2aMaps.copyMap(params));
        }
        values.put("attributes", WayangA2aMaps.copyMap(attributes));
        return WayangA2aMaps.copyMap(values);
    }
}

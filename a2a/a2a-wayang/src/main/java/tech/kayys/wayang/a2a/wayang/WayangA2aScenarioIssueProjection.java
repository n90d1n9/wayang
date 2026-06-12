package tech.kayys.wayang.a2a.wayang;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Shared ordered projection for compact A2A scenario failure summaries.
 */
final class WayangA2aScenarioIssueProjection {

    private WayangA2aScenarioIssueProjection() {
    }

    static String code(String value, String fallback) {
        String normalized = WayangA2aMaps.optional(value);
        return normalized == null ? WayangA2aMaps.required(fallback, "fallback") : normalized;
    }

    static String message(String value, String fallback) {
        String normalized = WayangA2aMaps.optional(value);
        return normalized == null ? WayangA2aMaps.required(fallback, "fallback") : normalized;
    }

    static String httpFallbackCode(int statusCode) {
        return "http_" + statusCode;
    }

    static String jsonRpcFallbackCode(int statusCode) {
        return statusCode >= 200 && statusCode < 300 ? "jsonrpc_error" : httpFallbackCode(statusCode);
    }

    static Map<String, Object> issue(
            String scenarioId,
            int exchangeIndex,
            Object requestId,
            String method,
            String path,
            int statusCode,
            String operation,
            String code,
            String message) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("scenarioId", WayangA2aMaps.required(scenarioId, "scenarioId"));
        values.put("exchangeIndex", exchangeIndex);
        if (requestId != null) {
            values.put("requestId", requestId);
        }
        values.put("method", WayangA2aMaps.required(method, "method"));
        if (path != null) {
            values.put("path", WayangA2aMaps.required(path, "path"));
        }
        values.put("statusCode", statusCode);
        String normalizedOperation = WayangA2aMaps.optional(operation);
        if (normalizedOperation != null) {
            values.put("operation", normalizedOperation);
        }
        values.put("code", WayangA2aMaps.required(code, "code"));
        values.put("message", WayangA2aMaps.required(message, "message"));
        return WayangA2aMaps.copyMap(values);
    }
}

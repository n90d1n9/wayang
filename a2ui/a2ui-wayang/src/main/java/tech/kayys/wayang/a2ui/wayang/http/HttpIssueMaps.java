package tech.kayys.wayang.a2ui.wayang.http;

import tech.kayys.wayang.a2ui.wayang.transport.TransportMaps;

import tech.kayys.wayang.a2ui.wayang.support.DecodeValues;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Small ordered issue-map builders shared by A2UI HTTP projections.
 */
public final class HttpIssueMaps {

    public static Map<String, Object> operationIssue(
            String source,
            String field,
            String operation,
            String message) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("source", source);
        values.put("field", field);
        values.put("operation", operation);
        values.put("message", message);
        return TransportMaps.freeze(values);
    }

    public static Map<String, Object> copiedIssueWithSource(Map<?, ?> issue, String source) {
        Map<String, Object> values = new LinkedHashMap<>(TransportMaps.copy(issue));
        values.putIfAbsent("source", source);
        return TransportMaps.freeze(values);
    }

    public static Map<String, Object> probeFailure(
            String code,
            String message,
            int statusCode,
            String routeOperation) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("code", code);
        values.put("message", message);
        values.put("statusCode", Math.max(0, statusCode));
        values.put("routeOperation", routeOperation == null ? "" : routeOperation);
        return TransportMaps.freeze(values);
    }

    public static Map<String, Object> probeAttributes(int statusCode, String routeOperation) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("statusCode", Math.max(0, statusCode));
        values.put("routeOperation", routeOperation == null ? "" : routeOperation);
        return TransportMaps.freeze(values);
    }

    public static List<String> issueValues(List<? extends Map<?, ?>> issues, String key) {
        if (issues == null || issues.isEmpty()) {
            return List.of();
        }
        Set<String> values = new LinkedHashSet<>();
        for (Map<?, ?> issue : issues) {
            String value = DecodeValues.text(issue == null ? null : issue.get(key));
            if (!value.isBlank()) {
                values.add(value);
            }
        }
        return List.copyOf(values);
    }

    private HttpIssueMaps() {
    }
}

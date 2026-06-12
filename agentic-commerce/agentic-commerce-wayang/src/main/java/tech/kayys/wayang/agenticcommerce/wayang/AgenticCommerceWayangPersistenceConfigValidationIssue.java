package tech.kayys.wayang.agenticcommerce.wayang;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One persistence configuration validation finding.
 */
public record AgenticCommerceWayangPersistenceConfigValidationIssue(
        String severity,
        String code,
        String path,
        String message,
        Map<String, Object> attributes) {

    public static final String SEVERITY_ERROR = "error";
    public static final String SEVERITY_WARNING = "warning";

    public AgenticCommerceWayangPersistenceConfigValidationIssue {
        severity = normalizeSeverity(severity);
        code = AgenticCommerceWayangMaps.required(code, "validation issue code");
        path = normalizePath(path);
        message = AgenticCommerceWayangMaps.text(message);
        attributes = AgenticCommerceWayangMaps.copy(attributes);
    }

    public static AgenticCommerceWayangPersistenceConfigValidationIssue error(
            String code,
            String path,
            String message,
            Map<String, Object> attributes) {
        return new AgenticCommerceWayangPersistenceConfigValidationIssue(
                SEVERITY_ERROR,
                code,
                path,
                message,
                attributes);
    }

    public static AgenticCommerceWayangPersistenceConfigValidationIssue warning(
            String code,
            String path,
            String message,
            Map<String, Object> attributes) {
        return new AgenticCommerceWayangPersistenceConfigValidationIssue(
                SEVERITY_WARNING,
                code,
                path,
                message,
                attributes);
    }

    public boolean error() {
        return SEVERITY_ERROR.equals(severity);
    }

    public boolean warning() {
        return SEVERITY_WARNING.equals(severity);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("severity", severity);
        values.put("code", code);
        values.put("path", path);
        values.put("message", message);
        values.put("attributes", attributes);
        return Map.copyOf(values);
    }

    private static String normalizeSeverity(String value) {
        String normalized = AgenticCommerceWayangMaps.text(value);
        if (SEVERITY_ERROR.equals(normalized) || SEVERITY_WARNING.equals(normalized)) {
            return normalized;
        }
        return SEVERITY_ERROR;
    }

    private static String normalizePath(String value) {
        String normalized = AgenticCommerceWayangMaps.text(value);
        return normalized.isBlank() ? "$" : normalized;
    }
}

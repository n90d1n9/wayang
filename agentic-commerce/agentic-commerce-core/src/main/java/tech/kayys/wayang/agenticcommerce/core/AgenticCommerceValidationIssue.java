package tech.kayys.wayang.agenticcommerce.core;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One validation issue for an Agentic Commerce HTTP request.
 */
public record AgenticCommerceValidationIssue(
        String code,
        String field,
        String message,
        String expected,
        String actual) {

    public AgenticCommerceValidationIssue {
        code = code == null || code.isBlank() ? "agentic_commerce_validation_error" : code.trim();
        field = field == null ? "" : field.trim();
        message = message == null || message.isBlank() ? code : message.trim();
        expected = expected == null ? "" : expected.trim();
        actual = actual == null ? "" : actual.trim();
    }

    public static AgenticCommerceValidationIssue of(
            String code,
            String field,
            String message,
            String expected,
            String actual) {
        return new AgenticCommerceValidationIssue(code, field, message, expected, actual);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("code", code);
        values.put("field", field);
        values.put("message", message);
        values.put("expected", expected);
        values.put("actual", actual);
        return AgenticCommerceMaps.copy(values);
    }
}

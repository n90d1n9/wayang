package tech.kayys.wayang.a2ui.wayang;

import java.util.Map;

/**
 * Validation issue emitted when an HTTP harness result does not match expectations.
 */
public record WayangA2uiHttpExpectationIssue(
        String targetId,
        String field,
        String expected,
        String actual,
        String message,
        Map<String, Object> attributes) {

    public WayangA2uiHttpExpectationIssue {
        targetId = targetId == null || targetId.isBlank() ? "a2ui-http-target" : targetId.trim();
        field = field == null || field.isBlank() ? "expectation" : field.trim();
        expected = expected == null ? "" : expected.trim();
        actual = actual == null ? "" : actual.trim();
        message = message == null || message.isBlank()
                ? "A2UI HTTP harness expectation did not match."
                : message.trim();
        attributes = WayangA2uiTransportMaps.copy(attributes);
    }

    public static WayangA2uiHttpExpectationIssue of(
            String targetId,
            String field,
            Object expected,
            Object actual,
            String message) {
        return new WayangA2uiHttpExpectationIssue(
                targetId,
                field,
                string(expected),
                string(actual),
                message,
                Map.of());
    }

    public Map<String, Object> toMap() {
        return WayangA2uiHttpExpectationProjection.issue(this);
    }

    private static String string(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}

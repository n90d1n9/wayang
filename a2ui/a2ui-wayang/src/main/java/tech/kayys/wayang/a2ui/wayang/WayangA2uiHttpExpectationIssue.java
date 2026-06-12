package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.wayang.http.HttpExpectationProjection;
import tech.kayys.wayang.a2ui.wayang.transport.TransportMaps;

import tech.kayys.wayang.a2ui.wayang.support.RecordValues;

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
        targetId = RecordValues.textOrDefault(targetId, "a2ui-http-target");
        field = RecordValues.textOrDefault(field, "expectation");
        expected = RecordValues.text(expected);
        actual = RecordValues.text(actual);
        message = RecordValues.textOrDefault(
                message,
                "A2UI HTTP harness expectation did not match.");
        attributes = TransportMaps.copy(attributes);
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
        return HttpExpectationProjection.issue(this);
    }

    private static String string(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}

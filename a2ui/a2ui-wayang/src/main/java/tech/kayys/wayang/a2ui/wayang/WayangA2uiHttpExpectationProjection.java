package tech.kayys.wayang.a2ui.wayang;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Ordered projections for A2UI HTTP expectation validation reports.
 */
final class WayangA2uiHttpExpectationProjection {

    private WayangA2uiHttpExpectationProjection() {
    }

    static Map<String, Object> result(WayangA2uiHttpExpectationResult result) {
        WayangA2uiHttpExpectationResult resolved = Objects.requireNonNull(result, "result");
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("targetId", resolved.targetId());
        values.put("expectationId", resolved.expectationId());
        values.put("passed", resolved.passed());
        values.put("issueCount", resolved.issueCount());
        values.put("validationIssues", resolved.validationIssues());
        values.put("attributes", resolved.attributes());
        return WayangA2uiTransportMaps.freeze(values);
    }

    static Map<String, Object> issue(WayangA2uiHttpExpectationIssue issue) {
        WayangA2uiHttpExpectationIssue resolved = Objects.requireNonNull(issue, "issue");
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("targetId", resolved.targetId());
        values.put("field", resolved.field());
        values.put("expected", resolved.expected());
        values.put("actual", resolved.actual());
        values.put("message", resolved.message());
        values.put("attributes", resolved.attributes());
        return WayangA2uiTransportMaps.freeze(values);
    }
}

package tech.kayys.wayang.a2ui.wayang;

import java.util.List;
import java.util.Map;

/**
 * Stable result for validating an A2UI HTTP scenario or suite against expectations.
 */
public record WayangA2uiHttpExpectationResult(
        String targetId,
        String expectationId,
        List<Map<String, Object>> validationIssues,
        Map<String, Object> attributes) {

    public WayangA2uiHttpExpectationResult {
        targetId = targetId == null || targetId.isBlank() ? "a2ui-http-target" : targetId.trim();
        expectationId = expectationId == null || expectationId.isBlank()
                ? "a2ui-http-expectation"
                : expectationId.trim();
        validationIssues = WayangA2uiTransportMaps.copyMaps(validationIssues);
        attributes = WayangA2uiTransportMaps.copy(attributes);
    }

    public static WayangA2uiHttpExpectationResult of(
            String targetId,
            String expectationId,
            List<WayangA2uiHttpExpectationIssue> issues,
            Map<?, ?> attributes) {
        return new WayangA2uiHttpExpectationResult(
                targetId,
                expectationId,
                issues == null
                        ? List.of()
                        : issues.stream()
                                .map(WayangA2uiHttpExpectationIssue::toMap)
                                .toList(),
                WayangA2uiTransportMaps.copy(attributes));
    }

    public boolean passed() {
        return validationIssues.isEmpty();
    }

    public int issueCount() {
        return validationIssues.size();
    }

    public Map<String, Object> toMap() {
        return WayangA2uiHttpExpectationProjection.result(this);
    }

    public String toJson() {
        return WayangA2uiTransportJson.json(toMap(), "Unable to encode A2UI HTTP expectation result");
    }
}

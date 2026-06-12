package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.wayang.http.HttpExpectationProjection;
import tech.kayys.wayang.a2ui.wayang.transport.TransportJson;
import tech.kayys.wayang.a2ui.wayang.transport.TransportMaps;

import tech.kayys.wayang.a2ui.wayang.support.RecordValues;

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
        targetId = RecordValues.textOrDefault(targetId, "a2ui-http-target");
        expectationId = RecordValues.textOrDefault(expectationId, "a2ui-http-expectation");
        validationIssues = TransportMaps.copyMaps(validationIssues);
        attributes = TransportMaps.copy(attributes);
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
                TransportMaps.copy(attributes));
    }

    public boolean passed() {
        return validationIssues.isEmpty();
    }

    public int issueCount() {
        return validationIssues.size();
    }

    public Map<String, Object> toMap() {
        return HttpExpectationProjection.result(this);
    }

    public String toJson() {
        return TransportJson.json(toMap(), "Unable to encode A2UI HTTP expectation result");
    }
}

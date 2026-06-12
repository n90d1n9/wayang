package tech.kayys.wayang.a2ui.wayang.http;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpExpectationIssue;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpExpectationResult;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HttpExpectationProjectionTest {

    @Test
    void projectsOrderedExpectationIssueAndRecordDelegates() {
        WayangA2uiHttpExpectationIssue issue = new WayangA2uiHttpExpectationIssue(
                "broken-exchange",
                "statusCodes",
                "[200]",
                "[400]",
                "Expected statusCodes to match exactly.",
                Map.of("source", "projection-test"));

        Map<String, Object> values = HttpExpectationProjection.issue(issue);

        assertThat(issue.toMap()).isEqualTo(values);
        assertThat(values.keySet()).containsExactly(
                "targetId",
                "field",
                "expected",
                "actual",
                "message",
                "attributes");
        assertThat(values)
                .containsEntry("targetId", "broken-exchange")
                .containsEntry("field", "statusCodes")
                .containsEntry("expected", "[200]")
                .containsEntry("actual", "[400]")
                .containsEntry("message", "Expected statusCodes to match exactly.");
        assertThat((Map<String, Object>) values.get("attributes"))
                .containsEntry("source", "projection-test");
    }

    @Test
    void projectsOrderedExpectationResultAndRecordDelegates() {
        WayangA2uiHttpExpectationIssue issue = WayangA2uiHttpExpectationIssue.of(
                "broken-exchange",
                "statusCodes",
                List.of(200),
                List.of(400),
                "Expected statusCodes to match exactly.");
        WayangA2uiHttpExpectationResult result = WayangA2uiHttpExpectationResult.of(
                "broken-exchange",
                "a2ui-http-scenario-pass",
                List.of(issue),
                Map.of("source", "projection-test"));

        Map<String, Object> values = HttpExpectationProjection.result(result);

        assertThat(result.toMap()).isEqualTo(values);
        assertThat(values.keySet()).containsExactly(
                "targetId",
                "expectationId",
                "passed",
                "issueCount",
                "validationIssues",
                "attributes");
        assertThat(values)
                .containsEntry("targetId", "broken-exchange")
                .containsEntry("expectationId", "a2ui-http-scenario-pass")
                .containsEntry("passed", false)
                .containsEntry("issueCount", 1);
        assertThat((Iterable<Map<String, Object>>) values.get("validationIssues"))
                .singleElement()
                .satisfies(projectedIssue -> assertThat(projectedIssue.keySet()).containsExactly(
                        "targetId",
                        "field",
                        "expected",
                        "actual",
                        "message",
                        "attributes"));
        assertThat((Map<String, Object>) values.get("attributes"))
                .containsEntry("source", "projection-test");
    }

    @Test
    void projectsPassingExpectationResultWithEmptyIssueList() {
        WayangA2uiHttpExpectationResult result = WayangA2uiHttpExpectationResult.of(
                "route-catalog",
                "a2ui-http-route-catalog-pass",
                List.of(),
                Map.of());

        Map<String, Object> values = HttpExpectationProjection.result(result);

        assertThat(values)
                .containsEntry("passed", true)
                .containsEntry("issueCount", 0)
                .containsEntry("validationIssues", List.of())
                .containsEntry("attributes", Map.of());
    }
}

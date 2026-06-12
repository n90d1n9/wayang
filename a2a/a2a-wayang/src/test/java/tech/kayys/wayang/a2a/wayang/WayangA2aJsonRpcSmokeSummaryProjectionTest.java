package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aJsonRpcSmokeSummaryProjectionTest {

    @Test
    void parsesScenarioResultIssuesWithSourceAndFallbackFields() {
        Map<String, Object> issue = new LinkedHashMap<>();
        issue.put("code", String.valueOf(WayangA2aJsonRpcError.METHOD_NOT_FOUND));
        issue.put("method", "UnknownMethod");
        Map<String, Object> scenarioResult = new LinkedHashMap<>();
        scenarioResult.put("scenarioId", "a2a.jsonrpc.summary");
        scenarioResult.put("passed", false);
        scenarioResult.put("exchangeCount", 1);
        scenarioResult.put("issueCount", 1);
        scenarioResult.put("issues", List.of(issue));
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("scenarioId", "a2a.jsonrpc.summary");
        attributes.put("exchangeCount", 1);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("passed", false);
        body.put("exitCode", WayangA2aJsonRpcSmokeResult.EXIT_FAILURE);
        body.put("scenarioResult", scenarioResult);
        body.put("attributes", attributes);

        WayangA2aJsonRpcSmokeSummary summary = WayangA2aJsonRpcSmokeSummaryProjection.fromMap(body);

        assertThat(summary.passed()).isFalse();
        assertThat(summary.successfulExit()).isFalse();
        assertThat(summary.exitCode()).isEqualTo(WayangA2aJsonRpcSmokeResult.EXIT_FAILURE);
        assertThat(summary.scenarioId()).isEqualTo("a2a.jsonrpc.summary");
        assertThat(summary.exchangeCount()).isEqualTo(1);
        assertThat(summary.issueCount()).isEqualTo(1);
        assertThat(summary.issues()).singleElement().satisfies(value -> {
            assertThat(value).containsEntry("code", String.valueOf(WayangA2aJsonRpcError.METHOD_NOT_FOUND));
            assertThat(value).containsEntry("method", "UnknownMethod");
            assertThat(value).containsEntry("source", "scenario");
        });
        assertThat(summary.body().keySet()).containsExactly("passed", "exitCode", "scenarioResult", "attributes");
    }

    @Test
    void keepsOrderedSummaryEnvelope() {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("scenarioId", "a2a.jsonrpc.summary");
        attributes.put("exchangeCount", 1);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("kind", "summary-body");

        Map<String, Object> values = WayangA2aJsonRpcSmokeSummaryProjection.summary(
                true,
                WayangA2aJsonRpcSmokeResult.EXIT_SUCCESS,
                "a2a.jsonrpc.summary",
                1,
                0,
                true,
                true,
                List.of(),
                attributes,
                body);

        assertThat(values.keySet()).containsExactly(
                "passed",
                "exitCode",
                "scenarioId",
                "exchangeCount",
                "issueCount",
                "smokeResult",
                "successfulExit",
                "issues",
                "attributes",
                "body");
        assertThat(values)
                .containsEntry("passed", true)
                .containsEntry("exitCode", WayangA2aJsonRpcSmokeResult.EXIT_SUCCESS)
                .containsEntry("scenarioId", "a2a.jsonrpc.summary")
                .containsEntry("successfulExit", true);
        assertThat(map(values.get("attributes")).keySet()).containsExactly("scenarioId", "exchangeCount");
        assertThat(map(values.get("body")).keySet()).containsExactly("kind");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value) {
        return (Map<String, Object>) value;
    }
}

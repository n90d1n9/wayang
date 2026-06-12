package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aJsonRpcReadinessIssueEnvelopeTest {

    @Test
    void wrapsProbeIssueWithSummaryEnvelopeAndMetadata() {
        Map<String, Object> issue = WayangA2aJsonRpcReadinessIssueEnvelope.wrap(
                "bindingReport",
                Map.of(
                        "source", "http",
                        "code", "json_content_mismatch",
                        "field", "contentType",
                        "expected", WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON,
                        "actual", "text/plain",
                        "message", "A2A JSON-RPC response was not JSON.",
                        "statusCode", 415,
                        "routeOperation", "JsonRpc",
                        "detail", "fallback"));
        String issueJson = WayangA2aHttpJson.write(issue);

        assertThat(issue)
                .containsEntry("probe", "bindingReport")
                .containsEntry("source", "http")
                .containsEntry("code", "json_content_mismatch")
                .containsEntry("field", "contentType")
                .containsEntry("expected", WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON)
                .containsEntry("actual", "text/plain")
                .containsEntry("statusCode", 415)
                .containsEntry("routeOperation", "JsonRpc");
        assertThat(WayangA2aMaps.copyMap((Map<?, ?>) issue.get("metadata")))
                .containsEntry("detail", "fallback")
                .doesNotContainKeys("source", "code", "field", "expected", "actual", "message");
        assertThat(issueJson).startsWith("{\"probe\":");
        assertThat(issueJson.indexOf("\"metadata\""))
                .isGreaterThan(issueJson.indexOf("\"routeOperation\""));
    }

    @Test
    void wrapsIssueListsInOrder() {
        List<Map<String, Object>> issues = WayangA2aJsonRpcReadinessIssueEnvelope.wrapAll(
                "smoke",
                List.of(
                        Map.of("code", "first"),
                        Map.of("code", "second")));

        assertThat(issues)
                .extracting(issue -> issue.get("code"))
                .containsExactly("first", "second");
        assertThat(issues)
                .allSatisfy(issue -> assertThat(issue).containsEntry("probe", "smoke"));
    }

    @Test
    void wrapsMissingIssueListsAsEmpty() {
        assertThat(WayangA2aJsonRpcReadinessIssueEnvelope.wrapAll("smoke", null)).isEmpty();
        assertThat(WayangA2aJsonRpcReadinessIssueEnvelope.wrapAll("smoke", List.of())).isEmpty();
    }
}

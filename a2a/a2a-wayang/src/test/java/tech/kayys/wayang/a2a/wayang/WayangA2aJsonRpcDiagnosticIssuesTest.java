package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aJsonRpcDiagnosticIssuesTest {

    @Test
    void buildsCanonicalDiagnosticIssueMap() {
        Map<String, Object> issue = WayangA2aJsonRpcDiagnosticIssues.issue(
                "bindingReport",
                "method_count_missing",
                "methodCount",
                "> 0",
                "0",
                "A2A JSON-RPC binding report did not expose any methods.");

        assertThat(issue)
                .containsEntry("source", "bindingReport")
                .containsEntry("code", "method_count_missing")
                .containsEntry("field", "methodCount")
                .containsEntry("expected", "> 0")
                .containsEntry("actual", "0")
                .containsEntry("message", "A2A JSON-RPC binding report did not expose any methods.");
        assertThat(WayangA2aHttpJson.write(issue)).startsWith("{\"source\":");
    }

    @Test
    void defaultsNullActualToBlankText() {
        assertThat(WayangA2aJsonRpcDiagnosticIssues.issue(
                "routeCatalog",
                "json_content_mismatch",
                "contentType",
                WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON,
                null,
                "A2A JSON-RPC route catalog response was not JSON."))
                .containsEntry("actual", "");
    }

    @Test
    void buildsCompactProbeFailureIssueMap() {
        Map<String, Object> issue = WayangA2aJsonRpcDiagnosticIssues.probeIssue(
                "binding_report_probe_failed",
                "A2A JSON-RPC binding report probe did not pass.",
                404,
                "JsonRpc");

        assertThat(issue)
                .containsEntry("code", "binding_report_probe_failed")
                .containsEntry("message", "A2A JSON-RPC binding report probe did not pass.")
                .containsEntry("statusCode", 404)
                .containsEntry("routeOperation", "JsonRpc")
                .doesNotContainKeys("source", "field", "expected", "actual");
        assertThat(WayangA2aHttpJson.write(issue)).startsWith("{\"code\":");
    }
}

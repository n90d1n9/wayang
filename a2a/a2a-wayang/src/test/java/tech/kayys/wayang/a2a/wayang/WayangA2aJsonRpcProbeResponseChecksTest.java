package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aJsonRpcProbeResponseChecksTest {

    @Test
    void buildsResponseIssuesForHttpRouteAndContentFailures() {
        WayangA2aHttpResponse response = new WayangA2aHttpResponse(
                404,
                "text/plain",
                "",
                Map.of(WayangA2aHttpResponse.HEADER_A2A_ROUTE_OPERATION, "JsonRpc"));

        assertThat(WayangA2aJsonRpcProbeResponseChecks.responseIssues(
                response,
                "bindingReport",
                WayangA2aJsonRpcBindingReport.OPERATION_JSON_RPC_BINDING_REPORT,
                "binding_report_route_mismatch",
                "binding report response"))
                .extracting(issue -> issue.get("code"))
                .containsExactly("http_unsuccessful", "binding_report_route_mismatch", "json_content_mismatch");
    }

    @Test
    void buildsCountMissingIssueWithProbeSubject() {
        assertThat(WayangA2aJsonRpcProbeResponseChecks.countMissingIssue(
                "routeCatalog",
                "route_count_missing",
                "routeCount",
                0,
                "route catalog",
                "routes"))
                .containsEntry("source", "routeCatalog")
                .containsEntry("code", "route_count_missing")
                .containsEntry("field", "routeCount")
                .containsEntry("expected", "> 0")
                .containsEntry("actual", "0")
                .containsEntry("message", "A2A JSON-RPC route catalog did not expose any routes.");
    }
}

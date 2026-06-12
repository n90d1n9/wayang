package tech.kayys.wayang.a2ui.wayang.http;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpEndpointDiagnosticIssueCatalog;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpRoute;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HttpIssueMapsTest {

    @Test
    void buildsOrderedOperationIssues() {
        Map<String, Object> issue = HttpIssueMaps.operationIssue(
                "bindingReport",
                "missingHandlerOperations",
                WayangA2uiHttpRoute.OPERATION_SMOKE,
                "A2UI HTTP route has no registered handler.");

        assertThat(issue.keySet()).containsExactly("source", "field", "operation", "message");
        assertThat(issue)
                .containsEntry("source", "bindingReport")
                .containsEntry("field", "missingHandlerOperations")
                .containsEntry("operation", WayangA2uiHttpRoute.OPERATION_SMOKE)
                .containsEntry("message", "A2UI HTTP route has no registered handler.");
    }

    @Test
    void copiesIssueAndOnlyAddsMissingSource() {
        Map<String, Object> withoutSource =
                HttpIssueMaps.copiedIssueWithSource(Map.of("message", "failed"), "suite");
        Map<String, Object> withSource =
                HttpIssueMaps.copiedIssueWithSource(Map.of(
                        "source",
                        "existing",
                        "message",
                        "failed"), "suite");

        assertThat(withoutSource).containsEntry("source", "suite");
        assertThat(withSource).containsEntry("source", "existing");
    }

    @Test
    void normalizesProbeFailureStatusAndRouteAttributes() {
        assertThat(HttpIssueMaps.probeFailure("failed", "Probe failed.", -1, null))
                .containsEntry("statusCode", 0)
                .containsEntry("routeOperation", "");
        assertThat(HttpIssueMaps.probeAttributes(-5, null))
                .containsEntry("statusCode", 0)
                .containsEntry("routeOperation", "");
    }

    @Test
    void extractsDistinctIssueValuesInEncounterOrder() {
        List<String> values = HttpIssueMaps.issueValues(
                List.of(
                        Map.of("category", WayangA2uiHttpEndpointDiagnosticIssueCatalog.CATEGORY_ROUTE_MISMATCH),
                        Map.of("category", WayangA2uiHttpEndpointDiagnosticIssueCatalog.CATEGORY_UNKNOWN_PATH),
                        Map.of("category", WayangA2uiHttpEndpointDiagnosticIssueCatalog.CATEGORY_ROUTE_MISMATCH),
                        Map.of("category", " ")),
                "category");

        assertThat(values).containsExactly(
                WayangA2uiHttpEndpointDiagnosticIssueCatalog.CATEGORY_ROUTE_MISMATCH,
                WayangA2uiHttpEndpointDiagnosticIssueCatalog.CATEGORY_UNKNOWN_PATH);
    }
}

package tech.kayys.wayang.a2ui.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2ui.wayang.http.HttpBindingReportProbeProjection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static tech.kayys.wayang.a2ui.wayang.WayangA2uiContractFixtures.contractBindingReportHttpResponse;
import static tech.kayys.wayang.a2ui.wayang.WayangA2uiContractFixtures.incompleteContractBindingReportHttpResponse;

class HttpBindingReportProbeProjectionTest {

    @Test
    void projectsOrderedProbeEnvelopeAndRecordDelegates() {
        WayangA2uiHttpBindingReportProbeResult probe = WayangA2uiHttpBindingReportProbeResult.from(
                contractBindingReportHttpResponse());

        Map<String, Object> values = HttpBindingReportProbeProjection.bindingReport(probe);

        assertThat(probe.toMap()).isEqualTo(values);
        assertThat(values.keySet()).containsExactly(
                "statusCode",
                "httpSuccessful",
                "routeOperation",
                "allow",
                WayangA2uiTransportFields.OUTCOME,
                "contentType",
                WayangA2uiTransportFields.MIME_TYPE,
                WayangA2uiTransportFields.BODY_ENCODING,
                "bindingReportRoute",
                "bindingReportResult",
                "jsonContent",
                WayangA2uiTransportFields.COMPLETE,
                WayangA2uiTransportFields.PASSED,
                WayangA2uiTransportFields.ROUTE_OPERATION_COUNT,
                WayangA2uiTransportFields.HANDLER_OPERATION_COUNT,
                WayangA2uiTransportFields.MISSING_HANDLER_COUNT,
                WayangA2uiTransportFields.ORPHAN_HANDLER_COUNT,
                WayangA2uiTransportFields.ISSUE_COUNT,
                "issues",
                "routeOperations",
                "handlerOperations",
                "missingHandlerOperations",
                "orphanHandlerOperations",
                WayangA2uiTransportFields.METADATA,
                WayangA2uiTransportFields.BODY,
                "headers");
        assertThat(values)
                .containsEntry("statusCode", 200)
                .containsEntry("routeOperation", WayangA2uiHttpRoute.OPERATION_BINDING_REPORT)
                .containsEntry(WayangA2uiTransportFields.PASSED, true)
                .containsEntry(WayangA2uiTransportFields.ROUTE_OPERATION_COUNT, 6);
    }

    @Test
    void projectsDiagnosticIssuesInCatalogThenHandlerOrder() {
        List<Map<String, Object>> issues = HttpBindingReportProbeProjection.issues(
                List.of(
                        WayangA2uiHttpRoute.OPERATION_SURFACE_CATALOG,
                        WayangA2uiHttpRoute.OPERATION_BINDING_REPORT),
                List.of(
                        WayangA2uiHttpRoute.OPERATION_ROUTE_CATALOG,
                        "a2ui.customHandler"));

        assertThat(issues).hasSize(4);
        assertThat(issues.getFirst())
                .containsEntry("source", "bindingReport")
                .containsEntry("field", "missingHandlerOperations")
                .containsEntry("operation", WayangA2uiHttpRoute.OPERATION_SURFACE_CATALOG)
                .containsEntry("message", "A2UI HTTP route has no registered handler.");
        assertThat(issues.getLast())
                .containsEntry("source", "bindingReport")
                .containsEntry("field", "orphanHandlerOperations")
                .containsEntry("operation", "a2ui.customHandler")
                .containsEntry("message", "A2UI HTTP handler is not declared by the route catalog.");
    }

    @Test
    void ignoresNullOperationsWhenProjectingIssues() {
        List<String> missingOperations = new ArrayList<>();
        missingOperations.add(null);
        missingOperations.add(WayangA2uiHttpRoute.OPERATION_SMOKE);
        List<String> orphanOperations = new ArrayList<>();
        orphanOperations.add(null);
        orphanOperations.add("a2ui.customHandler");

        List<Map<String, Object>> issues = HttpBindingReportProbeProjection.issues(
                missingOperations,
                orphanOperations);

        assertThat(issues).hasSize(2);
        assertThat(issues.getFirst())
                .containsEntry("field", "missingHandlerOperations")
                .containsEntry("operation", WayangA2uiHttpRoute.OPERATION_SMOKE);
        assertThat(issues.getLast())
                .containsEntry("field", "orphanHandlerOperations")
                .containsEntry("operation", "a2ui.customHandler");
    }

    @Test
    void incompleteProbeUsesProjectedIssues() {
        WayangA2uiHttpBindingReportProbeResult probe = WayangA2uiHttpBindingReportProbeResult.from(
                incompleteContractBindingReportHttpResponse());

        assertThat(probe.passed()).isFalse();
        assertThat(probe.issueCount()).isEqualTo(4);
        assertThat(probe.issues()).containsExactlyElementsOf(
                HttpBindingReportProbeProjection.issues(
                        probe.missingHandlerOperations(),
                        probe.orphanHandlerOperations()));
        assertThat(HttpBindingReportProbeProjection.bindingReport(probe))
                .containsEntry(WayangA2uiTransportFields.COMPLETE, false)
                .containsEntry(WayangA2uiTransportFields.PASSED, false)
                .containsEntry(WayangA2uiTransportFields.MISSING_HANDLER_COUNT, 2)
                .containsEntry(WayangA2uiTransportFields.ORPHAN_HANDLER_COUNT, 2);
    }
}

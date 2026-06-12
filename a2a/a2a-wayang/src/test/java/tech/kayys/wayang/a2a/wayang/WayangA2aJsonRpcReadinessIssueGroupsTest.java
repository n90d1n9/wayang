package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aJsonRpcReadinessIssueGroupsTest {

    @Test
    void collectsAndWrapsGroupsInSummaryOrder() {
        WayangA2aJsonRpcReadinessIssueGroups groups =
                WayangA2aJsonRpcReadinessIssueGroups.from(readinessWithCoverageIssues());

        assertThat(groups.readinessIssues()).hasSize(1);
        assertThat(groups.bindingReportIssues()).isEmpty();
        assertThat(groups.diagnosticHandlerIssues()).hasSize(1);
        assertThat(groups.methodDispatchIssues()).hasSize(2);
        assertThat(groups.routeCatalogIssues()).isEmpty();
        assertThat(groups.smokeIssues()).isEmpty();
        assertThat(groups.orderedGroups())
                .extracting(WayangA2aJsonRpcReadinessIssueGroup::probe)
                .containsExactly(
                        "readiness",
                        "bindingReport",
                        "diagnosticHandlers",
                        "methodDispatch",
                        "routeCatalog",
                        "smoke");
        assertThat(groups.wrappedIssues())
                .extracting(issue -> issue.get("probe"))
                .containsExactly("readiness", "diagnosticHandlers", "methodDispatch", "methodDispatch");
    }

    private static WayangA2aJsonRpcReadinessProbeResult readinessWithCoverageIssues() {
        Map<String, Object> body = new java.util.LinkedHashMap<>(
                WayangA2aJsonRpcBindingReport.defaults().toMap());
        body.put(
                "diagnosticHandlers",
                WayangA2aJsonRpcHttpDiagnosticHandlerCoverage.from(
                                WayangA2aJsonRpcHttpRouteDescriptor.fromConfig(
                                        WayangA2aJsonRpcHttpConfig.defaults()),
                                List.of())
                        .toMap());
        body.put(
                "methodDispatch",
                WayangA2aJsonRpcMethodDispatchCoverage.from(
                                List.of(
                                        WayangA2aJsonRpcMethods.SEND_MESSAGE,
                                        WayangA2aJsonRpcMethods.GET_TASK),
                                List.of(WayangA2aJsonRpcMethods.SEND_MESSAGE))
                        .toMap());
        return new WayangA2aJsonRpcReadinessProbeResult(
                WayangA2aJsonRpcBindingReportProbeResult.from(bindingReportResponse(body)),
                null,
                false,
                null,
                false);
    }

    private static WayangA2aHttpResponse bindingReportResponse(Map<String, Object> body) {
        return new WayangA2aHttpResponse(
                200,
                WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON,
                WayangA2aHttpJson.write(body),
                Map.of(
                        WayangA2aHttpResponse.HEADER_CONTENT_TYPE,
                        WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON,
                        WayangA2aHttpResponse.HEADER_A2A_ROUTE_OPERATION,
                        WayangA2aJsonRpcBindingReport.OPERATION_JSON_RPC_BINDING_REPORT,
                        WayangA2aHttpResponse.HEADER_A2A_PROTOCOL_VERSION,
                        "1.0"));
    }
}

package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aJsonRpcReadinessDiagnosticCheckAssemblyTest {

    @Test
    void assemblesSectionRowsInOperatorOrder() {
        WayangA2aJsonRpcReadinessProbeResult readiness = readinessWithCoverageAndRegistryRows();
        WayangA2aJsonRpcReadinessIssueBreakdown breakdown =
                WayangA2aJsonRpcReadinessIssueBreakdown.from(readiness);

        List<Map<String, Object>> rows =
                WayangA2aJsonRpcReadinessDiagnosticCheckAssembly.from(
                                readiness,
                                WayangA2aSpecAlignmentSnapshot.defaults(),
                                breakdown)
                        .toMaps();

        assertThat(rows)
                .extracting(row -> row.get("probe"))
                .containsExactly(
                        "bindingReport",
                        "routeCatalog",
                        "smoke",
                        "diagnosticHandlers",
                        "methodDispatch",
                        "methodRegistry",
                        "specAlignment",
                        "specAlignment:protocol",
                        "specAlignment:binding",
                        "specAlignment:agent_card",
                        "specAlignment:route",
                        "specAlignment:jsonrpc",
                        "readiness");
    }

    private static WayangA2aJsonRpcReadinessProbeResult readinessWithCoverageAndRegistryRows() {
        Map<String, Object> body = new LinkedHashMap<>(
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
        body.put(
                "methodRegistry",
                WayangA2aJsonRpcMethodRegistryTestFixtures.taskRegistryMap());
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

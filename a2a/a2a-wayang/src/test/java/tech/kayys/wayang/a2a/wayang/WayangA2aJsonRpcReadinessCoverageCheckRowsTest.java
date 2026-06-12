package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aJsonRpcReadinessCoverageCheckRowsTest {

    @Test
    void omitsRowsWhenNoCoverageIssuesAreReported() {
        WayangA2aJsonRpcReadinessCoverageCheckRows rows =
                WayangA2aJsonRpcReadinessCoverageCheckRows.from(new WayangA2aJsonRpcReadinessProbeResult(
                        WayangA2aJsonRpcBindingReportProbeResult.fromMap(Map.of()),
                        null,
                        false,
                        null,
                        false));

        assertThat(rows.toMaps()).isEmpty();
    }

    @Test
    void ordersDiagnosticHandlerRowBeforeMethodDispatchRow() {
        WayangA2aJsonRpcReadinessCoverageCheckRows rows =
                WayangA2aJsonRpcReadinessCoverageCheckRows.from(readinessWithCoverageRows());

        assertThat(rows.toMaps())
                .extracting(row -> row.get("probe"))
                .containsExactly("diagnosticHandlers", "methodDispatch");
        assertThat(rows.toMaps())
                .anySatisfy(row -> assertThat(row)
                        .containsEntry("probe", "diagnosticHandlers")
                        .containsEntry("passed", false)
                        .containsEntry("issueCount", 1))
                .anySatisfy(row -> assertThat(row)
                        .containsEntry("probe", "methodDispatch")
                        .containsEntry("passed", false)
                        .containsEntry("issueCount", 2));
    }

    @Test
    void canUsePrecomputedIssueBreakdown() {
        WayangA2aJsonRpcReadinessProbeResult readiness = readinessWithCoverageRows();
        WayangA2aJsonRpcReadinessIssueBreakdown breakdown =
                WayangA2aJsonRpcReadinessIssueBreakdown.from(readiness);

        assertThat(WayangA2aJsonRpcReadinessCoverageCheckRows.from(readiness, breakdown).toMaps())
                .isEqualTo(WayangA2aJsonRpcReadinessCoverageCheckRows.from(readiness).toMaps());
    }

    @Test
    void includesNonFailingMethodRegistryRowWhenReported() {
        WayangA2aJsonRpcReadinessCoverageCheckRows rows =
                WayangA2aJsonRpcReadinessCoverageCheckRows.from(readinessWithMethodRegistryRow());

        assertThat(rows.toMaps())
                .singleElement()
                .satisfies(row -> assertThat(row)
                        .containsEntry("probe", "methodRegistry")
                        .containsEntry("required", true)
                        .containsEntry("passed", true)
                        .containsEntry("issueCount", 0));
    }

    private static WayangA2aJsonRpcReadinessProbeResult readinessWithCoverageRows() {
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

    private static WayangA2aJsonRpcReadinessProbeResult readinessWithMethodRegistryRow() {
        Map<String, Object> body = new java.util.LinkedHashMap<>(
                WayangA2aJsonRpcBindingReport.defaults().toMap());
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

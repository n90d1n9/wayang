package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aJsonRpcBindingReportProbeIssuesTest {

    @Test
    void emitsCountAndRequiredSectionPathIssues() {
        WayangA2aJsonRpcBindingReportProbeIssues issues =
                WayangA2aJsonRpcBindingReportProbeIssues.from(
                        bindingReportResponse(),
                        0,
                        List.of(new WayangA2aJsonRpcBindingReportSection("bindingReport", "", true)),
                        completeDiagnosticCoverage(),
                        WayangA2aJsonRpcMethodDispatchCoverage.fromMap(Map.of()));

        assertThat(issues.issueCount()).isEqualTo(2);
        assertThat(issues.issues())
                .anySatisfy(issue -> assertThat(issue)
                        .containsEntry(
                                "code",
                                WayangA2aJsonRpcReadinessIssueCatalog.ISSUE_METHOD_COUNT_MISSING)
                        .containsEntry("field", "methodCount"))
                .anySatisfy(issue -> assertThat(issue)
                        .containsEntry("code", "binding_report_path_missing")
                        .containsEntry("field", "bindingReportPath"));
    }

    @Test
    void emitsDiagnosticHandlerAndMethodDispatchCoverageIssues() {
        WayangA2aJsonRpcBindingReportProbeIssues issues =
                WayangA2aJsonRpcBindingReportProbeIssues.from(
                        bindingReportResponse(),
                        1,
                        List.of(new WayangA2aJsonRpcBindingReportSection("bindingReport", "/a2a/bindings", true)),
                        new WayangA2aJsonRpcHttpDiagnosticHandlerCoverage(
                                true,
                                List.of("smoke", "routeCatalog"),
                                List.of("smoke", "custom"),
                                List.of("routeCatalog"),
                                List.of("custom")),
                        WayangA2aJsonRpcMethodDispatchCoverage.from(
                                List.of(
                                        WayangA2aJsonRpcMethods.SEND_MESSAGE,
                                        WayangA2aJsonRpcMethods.GET_TASK),
                                List.of(WayangA2aJsonRpcMethods.SEND_MESSAGE)));

        assertThat(issues.issues())
                .anySatisfy(issue -> assertThat(issue)
                        .containsEntry(
                                "code",
                                WayangA2aJsonRpcReadinessIssueCatalog
                                        .ISSUE_DIAGNOSTIC_HANDLER_COVERAGE_INCOMPLETE)
                        .containsEntry(
                                "field",
                                WayangA2aJsonRpcReadinessIssueCatalog.FIELD_DIAGNOSTIC_HANDLERS_COMPLETE)
                        .containsEntry("actual", "missing=[routeCatalog], orphan=[custom]"))
                .anySatisfy(issue -> assertThat(issue)
                        .containsEntry(
                                "code",
                                WayangA2aJsonRpcReadinessIssueCatalog
                                        .ISSUE_METHOD_DISPATCH_COVERAGE_INCOMPLETE)
                        .containsEntry(
                                "field",
                                WayangA2aJsonRpcReadinessIssueCatalog.FIELD_METHOD_DISPATCH_COMPLETE));
    }

    private static WayangA2aJsonRpcHttpDiagnosticHandlerCoverage completeDiagnosticCoverage() {
        return new WayangA2aJsonRpcHttpDiagnosticHandlerCoverage(
                true,
                List.of("smoke"),
                List.of("smoke"),
                List.of(),
                List.of());
    }

    private static WayangA2aHttpResponse bindingReportResponse() {
        return new WayangA2aHttpResponse(
                200,
                WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON,
                "{}",
                Map.of(
                        WayangA2aHttpResponse.HEADER_CONTENT_TYPE,
                        WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON,
                        WayangA2aHttpResponse.HEADER_A2A_ROUTE_OPERATION,
                        WayangA2aJsonRpcBindingReport.OPERATION_JSON_RPC_BINDING_REPORT));
    }
}

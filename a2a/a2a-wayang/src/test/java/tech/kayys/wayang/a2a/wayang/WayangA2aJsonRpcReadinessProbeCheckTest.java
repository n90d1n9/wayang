package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2a.core.A2aProtocol;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aJsonRpcReadinessProbeCheckTest {

    @Test
    void buildsFailureIssuesForRequiredFailedProbes() {
        WayangA2aJsonRpcReadinessProbeResult readiness =
                readinessWithFailedBindingReportProbe();

        assertThat(WayangA2aJsonRpcReadinessProbeCheck.issues(readiness))
                .singleElement()
                .satisfies(issue -> assertThat(issue)
                        .containsEntry("code", "binding_report_probe_failed")
                        .containsEntry("message", "A2A JSON-RPC binding report probe did not pass.")
                        .containsEntry("statusCode", 404)
                        .containsEntry("routeOperation", "JsonRpc"));
    }

    @Test
    void buildsDiagnosticCheckRowsFromReadinessState() {
        WayangA2aJsonRpcReadinessProbeResult readiness =
                readinessWithFailedBindingReportProbe();

        assertThat(WayangA2aJsonRpcReadinessProbeCheck.diagnosticChecks(readiness))
                .extracting(check -> check.get("probe"))
                .containsExactly(
                        "bindingReport",
                        "routeCatalog",
                        "smoke",
                        "specAlignment",
                        "specAlignment:protocol",
                        "specAlignment:binding",
                        "specAlignment:agent_card",
                        "specAlignment:route",
                        "specAlignment:jsonrpc",
                        "readiness");
        assertThat(WayangA2aJsonRpcReadinessProbeCheck.diagnosticChecks(readiness))
                .anySatisfy(check -> assertThat(check)
                        .containsEntry("probe", "bindingReport")
                        .containsEntry("required", true)
                        .containsEntry("passed", false)
                        .containsEntry("statusCode", 404)
                        .containsEntry("routeOperation", "JsonRpc"))
                .anySatisfy(check -> assertThat(check)
                        .containsEntry("probe", "routeCatalog")
                        .containsEntry("required", false)
                        .containsEntry("passed", true))
                .anySatisfy(check -> assertThat(check)
                        .containsEntry("probe", "specAlignment")
                        .containsEntry("required", true)
                        .containsEntry("passed", true)
                        .containsEntry("issueCount", 0))
                .anySatisfy(check -> assertThat(check)
                        .containsEntry("probe", "specAlignment:route")
                        .containsEntry("category", "route")
                        .containsEntry("passed", true)
                        .containsEntry("gapIds", List.of()))
                .anySatisfy(check -> assertThat(check)
                        .containsEntry("probe", "readiness")
                        .containsEntry("required", true)
                        .containsEntry("passed", false)
                        .containsEntry("issueCount", 1));
    }

    @Test
    void addsMethodDispatchDiagnosticRowWhenCoverageIsReported() {
        WayangA2aJsonRpcReadinessProbeResult readiness =
                readinessWithReportedMethodDispatch(false);

        assertThat(WayangA2aJsonRpcReadinessProbeCheck.diagnosticChecks(readiness))
                .extracting(check -> check.get("probe"))
                .containsExactly(
                        "bindingReport",
                        "routeCatalog",
                        "smoke",
                        "methodDispatch",
                        "specAlignment",
                        "specAlignment:protocol",
                        "specAlignment:binding",
                        "specAlignment:agent_card",
                        "specAlignment:route",
                        "specAlignment:jsonrpc",
                        "readiness");
        assertThat(WayangA2aJsonRpcReadinessProbeCheck.diagnosticChecks(readiness))
                .anySatisfy(check -> assertThat(check)
                        .containsEntry("probe", "methodDispatch")
                        .containsEntry("required", true)
                        .containsEntry("passed", false)
                        .containsEntry("issueCount", 2));
    }

    @Test
    void addsDiagnosticHandlersRowWhenConcreteCoverageIssuesExist() {
        WayangA2aJsonRpcReadinessProbeResult readiness =
                readinessWithConcreteDiagnosticHandlerIssue();

        assertThat(WayangA2aJsonRpcReadinessProbeCheck.diagnosticChecks(readiness))
                .extracting(check -> check.get("probe"))
                .containsExactly(
                        "bindingReport",
                        "routeCatalog",
                        "smoke",
                        "diagnosticHandlers",
                        "specAlignment",
                        "specAlignment:protocol",
                        "specAlignment:binding",
                        "specAlignment:agent_card",
                        "specAlignment:route",
                        "specAlignment:jsonrpc",
                        "readiness");
        assertThat(WayangA2aJsonRpcReadinessProbeCheck.diagnosticChecks(readiness))
                .anySatisfy(check -> assertThat(check)
                        .containsEntry("probe", "diagnosticHandlers")
                        .containsEntry("required", true)
                        .containsEntry("passed", false)
                        .containsEntry("issueCount", 1));
    }

    @Test
    void addsSpecAlignmentCategoryDiagnosticRows() {
        WayangA2aSpecAlignmentSnapshot specAlignment = new WayangA2aSpecAlignmentSnapshot(
                "a2a",
                A2aProtocol.VERSION,
                A2aProtocol.BINDING_JSONRPC,
                false,
                20,
                19,
                1,
                List.of("route.SendMessage"),
                List.of(new WayangA2aSpecAlignmentCategorySummary(
                        "route",
                        12,
                        11,
                        1,
                        List.of("route.SendMessage"))));

        assertThat(WayangA2aJsonRpcReadinessProbeCheck.diagnosticChecks(
                        readinessWithFailedBindingReportProbe(),
                        specAlignment))
                .anySatisfy(check -> assertThat(check)
                        .containsEntry("probe", "specAlignment:route")
                        .containsEntry("category", "route")
                        .containsEntry("required", true)
                        .containsEntry("passed", false)
                        .containsEntry("issueCount", 1)
                        .containsEntry("gapIds", List.of("route.SendMessage")));
    }

    private static WayangA2aJsonRpcReadinessProbeResult readinessWithFailedBindingReportProbe() {
        return new WayangA2aJsonRpcReadinessProbeResult(
                WayangA2aJsonRpcBindingReportProbeResult.fromMap(Map.of(
                        "statusCode", 404,
                        "routeOperation", "JsonRpc")),
                null,
                false,
                null,
                false);
    }

    private static WayangA2aJsonRpcReadinessProbeResult readinessWithReportedMethodDispatch(boolean complete) {
        List<String> registeredMethods = List.of(
                WayangA2aJsonRpcMethods.SEND_MESSAGE,
                WayangA2aJsonRpcMethods.GET_TASK);
        List<String> dispatchMethods = complete
                ? registeredMethods
                : List.of(WayangA2aJsonRpcMethods.SEND_MESSAGE);
        WayangA2aJsonRpcBindingReport report = new WayangA2aJsonRpcBindingReport(
                WayangA2aJsonRpcHttpConfig.defaults(),
                WayangA2aJsonRpcMethods.methods(),
                WayangA2aJsonRpcMethodDispatchCoverage.from(registeredMethods, dispatchMethods));
        return new WayangA2aJsonRpcReadinessProbeResult(
                WayangA2aJsonRpcBindingReportProbeResult.from(report.response()),
                null,
                false,
                null,
                false);
    }

    private static WayangA2aJsonRpcReadinessProbeResult readinessWithConcreteDiagnosticHandlerIssue() {
        return new WayangA2aJsonRpcReadinessProbeResult(
                WayangA2aJsonRpcBindingReportProbeResult.fromMap(Map.of(
                        "issues",
                        List.of(Map.of(
                                "source",
                                "bindingReport",
                                "code",
                                WayangA2aJsonRpcReadinessIssueCatalog
                                        .ISSUE_DIAGNOSTIC_HANDLER_COVERAGE_INCOMPLETE,
                                "field",
                                WayangA2aJsonRpcReadinessIssueCatalog
                                        .FIELD_DIAGNOSTIC_HANDLERS_COMPLETE,
                                "actual",
                                "missing=[readiness], orphan=[]")),
                        "issueCount",
                        1)),
                null,
                false,
                null,
                false);
    }
}

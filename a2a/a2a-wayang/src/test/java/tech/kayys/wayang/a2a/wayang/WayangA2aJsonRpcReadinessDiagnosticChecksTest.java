package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2a.core.A2aProtocol;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aJsonRpcReadinessDiagnosticChecksTest {

    @Test
    void assemblesDiagnosticRowsInOperatorOrder() {
        WayangA2aJsonRpcReadinessProbeResult readiness = readinessWithFailedBindingReportProbe();

        assertThat(WayangA2aJsonRpcReadinessDiagnosticChecks.from(readiness).toMaps())
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
    }

    @Test
    void keepsProbeCheckDelegateCompatible() {
        WayangA2aJsonRpcReadinessProbeResult readiness = readinessWithFailedBindingReportProbe();

        assertThat(WayangA2aJsonRpcReadinessProbeCheck.diagnosticChecks(readiness))
                .isEqualTo(WayangA2aJsonRpcReadinessDiagnosticChecks.from(readiness).toMaps());
    }

    @Test
    void delegatesRowAssemblyToCheckAssembly() {
        WayangA2aJsonRpcReadinessProbeResult readiness = readinessWithFailedBindingReportProbe();
        WayangA2aJsonRpcReadinessIssueBreakdown breakdown =
                WayangA2aJsonRpcReadinessIssueBreakdown.from(readiness);

        assertThat(WayangA2aJsonRpcReadinessDiagnosticChecks.from(
                        readiness,
                        WayangA2aSpecAlignmentSnapshot.defaults(),
                        breakdown)
                .toMaps())
                .isEqualTo(WayangA2aJsonRpcReadinessDiagnosticCheckAssembly.from(
                                readiness,
                                WayangA2aSpecAlignmentSnapshot.defaults(),
                                breakdown)
                        .toMaps());
    }

    @Test
    void includesSpecAlignmentCategoryRows() {
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

        assertThat(WayangA2aJsonRpcReadinessDiagnosticChecks.from(
                        readinessWithFailedBindingReportProbe(),
                        specAlignment)
                .toMaps())
                .anySatisfy(check -> assertThat(check)
                        .containsEntry("probe", "specAlignment:route")
                        .containsEntry("category", "route")
                        .containsEntry("required", true)
                        .containsEntry("passed", false)
                        .containsEntry("issueCount", 1)
                        .containsEntry("gapIds", List.of("route.SendMessage")));
        String categoryJson = WayangA2aHttpJson.write(WayangA2aJsonRpcReadinessDiagnosticChecks.from(
                        readinessWithFailedBindingReportProbe(),
                        specAlignment)
                .toMaps()
                .stream()
                .filter(check -> "specAlignment:route".equals(check.get("probe")))
                .findFirst()
                .orElseThrow());
        assertThat(categoryJson).startsWith("{\"probe\":");
        assertThat(categoryJson.indexOf("\"gapIds\""))
                .isGreaterThan(categoryJson.indexOf("\"issueCount\""));
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
}

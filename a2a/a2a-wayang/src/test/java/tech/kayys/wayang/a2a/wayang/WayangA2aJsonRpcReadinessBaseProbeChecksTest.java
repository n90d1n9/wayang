package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aJsonRpcReadinessBaseProbeChecksTest {

    @Test
    void buildsBaseProbeRowsInOperatorOrder() {
        WayangA2aJsonRpcReadinessBaseProbeChecks checks =
                WayangA2aJsonRpcReadinessBaseProbeChecks.from(readinessWithFailedBindingReportProbe());

        assertThat(checks.toMaps())
                .extracting(row -> row.get("probe"))
                .containsExactly("bindingReport", "routeCatalog", "smoke");
        assertThat(checks.toMaps())
                .anySatisfy(row -> assertThat(row)
                        .containsEntry("probe", "bindingReport")
                        .containsEntry("required", true)
                        .containsEntry("passed", false)
                        .containsEntry("statusCode", 404)
                        .containsEntry("routeOperation", "JsonRpc"))
                .anySatisfy(row -> assertThat(row)
                        .containsEntry("probe", "routeCatalog")
                        .containsEntry("required", false)
                        .containsEntry("passed", true))
                .anySatisfy(row -> assertThat(row)
                        .containsEntry("probe", "smoke")
                        .containsEntry("required", false)
                        .containsEntry("passed", true));
    }

    @Test
    void derivesFailureIssuesOnlyFromFailedRequiredBaseProbes() {
        assertThat(WayangA2aJsonRpcReadinessBaseProbeChecks.from(
                        readinessWithFailedBindingReportProbe())
                .issues())
                .singleElement()
                .satisfies(issue -> assertThat(issue)
                        .containsEntry("code", "binding_report_probe_failed")
                        .containsEntry("message", "A2A JSON-RPC binding report probe did not pass.")
                        .containsEntry("statusCode", 404)
                        .containsEntry("routeOperation", "JsonRpc"));
    }

    @Test
    void keepsProbeCheckDelegatesCompatible() {
        WayangA2aJsonRpcReadinessProbeResult readiness = readinessWithFailedBindingReportProbe();

        assertThat(WayangA2aJsonRpcReadinessProbeCheck.probeChecks(readiness))
                .isEqualTo(WayangA2aJsonRpcReadinessBaseProbeChecks.from(readiness).checks());
        assertThat(WayangA2aJsonRpcReadinessProbeCheck.issues(readiness))
                .isEqualTo(WayangA2aJsonRpcReadinessBaseProbeChecks.from(readiness).issues());
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

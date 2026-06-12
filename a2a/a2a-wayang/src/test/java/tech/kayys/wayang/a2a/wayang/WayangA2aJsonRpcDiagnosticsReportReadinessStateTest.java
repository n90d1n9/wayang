package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aJsonRpcDiagnosticsReportReadinessStateTest {

    @Test
    void capturesTopLevelReadinessBooleans() {
        WayangA2aJsonRpcDiagnosticsReportReadinessState state =
                WayangA2aJsonRpcDiagnosticsReportReadinessState.from(failedBindingReportReadiness());

        assertThat(state.bindingReportPassed()).isFalse();
        assertThat(state.routeCatalogRequired()).isFalse();
        assertThat(state.routeCatalogPassed()).isTrue();
        assertThat(state.smokeRequired()).isFalse();
        assertThat(state.smokePassed()).isTrue();
    }

    @Test
    void preservesReportFieldOrderInMap() {
        assertThat(WayangA2aJsonRpcDiagnosticsReportReadinessState.from(
                        failedBindingReportReadiness())
                .toMap()
                .keySet())
                .containsExactly(
                        "bindingReportPassed",
                        "routeCatalogRequired",
                        "routeCatalogPassed",
                        "smokeRequired",
                        "smokePassed");
    }

    private static WayangA2aJsonRpcReadinessProbeResult failedBindingReportReadiness() {
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

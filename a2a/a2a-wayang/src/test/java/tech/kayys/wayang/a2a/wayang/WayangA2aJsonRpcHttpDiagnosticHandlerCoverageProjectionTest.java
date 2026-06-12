package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aJsonRpcHttpDiagnosticHandlerCoverageProjectionTest {

    @Test
    void keepsOrderedCoverageEnvelope() {
        WayangA2aJsonRpcHttpDiagnosticHandlerCoverage coverage = defaultCoverage();

        Map<String, Object> values =
                WayangA2aJsonRpcHttpDiagnosticHandlerCoverageProjection.coverage(coverage);

        assertThat(values.keySet()).containsExactly(
                "complete",
                "routeKeyCount",
                "handlerKeyCount",
                "routeKeys",
                "handlerKeys",
                "missingHandlerKeys",
                "orphanHandlerKeys");
        assertThat(values)
                .containsEntry("complete", true)
                .containsEntry("routeKeyCount", 7)
                .containsEntry("handlerKeyCount", 7);
        assertThat(WayangA2aMaps.stringList(values.get("routeKeys"))).containsExactly(
                WayangA2aJsonRpcHttpRouteDescriptor.KEY_SMOKE,
                WayangA2aJsonRpcHttpRouteDescriptor.KEY_ROUTE_CATALOG,
                WayangA2aJsonRpcHttpRouteDescriptor.KEY_DIAGNOSTICS_REPORT,
                WayangA2aJsonRpcHttpRouteDescriptor.KEY_SPEC_COMPLIANCE_REPORT,
                WayangA2aJsonRpcHttpRouteDescriptor.KEY_BINDING_REPORT,
                WayangA2aJsonRpcHttpRouteDescriptor.KEY_READINESS,
                WayangA2aJsonRpcHttpRouteDescriptor.KEY_READINESS_ISSUE_SUMMARY);
    }

    @Test
    void parsesCoverageMapsThroughProjection() {
        WayangA2aJsonRpcHttpDiagnosticHandlerCoverage coverage =
                WayangA2aJsonRpcHttpDiagnosticHandlerCoverageProjection.fromMap(Map.of(
                        "routeKeys", List.of("smoke", "routeCatalog"),
                        "handlerKeys", List.of("smoke", "orphan"),
                        "missingHandlerKeys", List.of("routeCatalog"),
                        "orphanHandlerKeys", List.of("orphan")));

        assertThat(coverage.complete()).isFalse();
        assertThat(coverage.routeKeyCount()).isEqualTo(2);
        assertThat(coverage.handlerKeyCount()).isEqualTo(2);
        assertThat(coverage.routeKeys()).containsExactly("smoke", "routeCatalog");
        assertThat(coverage.handlerKeys()).containsExactly("smoke", "orphan");
        assertThat(coverage.missingHandlerKeys()).containsExactly("routeCatalog");
        assertThat(coverage.orphanHandlerKeys()).containsExactly("orphan");
    }

    @Test
    void ignoresStatusOnlyMapsWhenReportingCoverage() {
        WayangA2aJsonRpcHttpDiagnosticHandlerCoverage coverage =
                WayangA2aJsonRpcHttpDiagnosticHandlerCoverageProjection.fromMap(Map.of("complete", true));

        assertThat(coverage.complete()).isFalse();
        assertThat(coverage.routeKeys()).isEmpty();
        assertThat(coverage.handlerKeys()).isEmpty();
        assertThat(WayangA2aJsonRpcHttpDiagnosticHandlerCoverageProjection.coverage(coverage))
                .containsEntry("complete", false)
                .containsEntry("routeKeyCount", 0)
                .containsEntry("handlerKeyCount", 0);
    }

    @Test
    void recordDelegatesToProjectionForCoverageMap() {
        WayangA2aJsonRpcHttpDiagnosticHandlerCoverage coverage = defaultCoverage();

        assertThat(coverage.toMap())
                .isEqualTo(WayangA2aJsonRpcHttpDiagnosticHandlerCoverageProjection.coverage(coverage));
        assertThat(WayangA2aJsonRpcHttpDiagnosticHandlerCoverage.fromMap(coverage.toMap()).toMap())
                .isEqualTo(coverage.toMap());
    }

    private static WayangA2aJsonRpcHttpDiagnosticHandlerCoverage defaultCoverage() {
        return WayangA2aJsonRpcHttpDiagnosticHandlerCoverage.from(
                WayangA2aJsonRpcHttpRouteDescriptor.fromConfig(WayangA2aJsonRpcHttpConfig.defaults()),
                WayangA2aJsonRpcHttpDiagnosticHandlers.defaultHandlerKeys());
    }
}

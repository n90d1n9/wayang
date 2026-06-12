package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aJsonRpcHttpDiagnosticHandlerCoverageTest {

    @Test
    void reportsCompleteCoverageForDefaultRoutesAndHandlers() {
        WayangA2aJsonRpcHttpDiagnosticHandlerCoverage coverage =
                WayangA2aJsonRpcHttpDiagnosticHandlerCoverage.from(
                        WayangA2aJsonRpcHttpRouteDescriptor.fromConfig(WayangA2aJsonRpcHttpConfig.defaults()),
                        WayangA2aJsonRpcHttpDiagnosticHandlers.defaultHandlerKeys());

        assertThat(coverage.complete()).isTrue();
        assertThat(coverage.routeKeyCount()).isEqualTo(7);
        assertThat(coverage.handlerKeyCount()).isEqualTo(7);
        assertThat(coverage.routeKeys()).containsExactly(
                WayangA2aJsonRpcHttpRouteDescriptor.KEY_SMOKE,
                WayangA2aJsonRpcHttpRouteDescriptor.KEY_ROUTE_CATALOG,
                WayangA2aJsonRpcHttpRouteDescriptor.KEY_DIAGNOSTICS_REPORT,
                WayangA2aJsonRpcHttpRouteDescriptor.KEY_SPEC_COMPLIANCE_REPORT,
                WayangA2aJsonRpcHttpRouteDescriptor.KEY_BINDING_REPORT,
                WayangA2aJsonRpcHttpRouteDescriptor.KEY_READINESS,
                WayangA2aJsonRpcHttpRouteDescriptor.KEY_READINESS_ISSUE_SUMMARY);
        assertThat(coverage.missingHandlerKeys()).isEmpty();
        assertThat(coverage.orphanHandlerKeys()).isEmpty();
        assertThat(coverage.toMap())
                .containsEntry("complete", true)
                .containsEntry("routeKeyCount", 7)
                .containsEntry("handlerKeyCount", 7);
        String coverageJson = WayangA2aHttpJson.write(coverage.toMap());
        assertThat(coverageJson).startsWith("{\"complete\":");
        assertThat(coverageJson.indexOf("\"routeKeys\""))
                .isGreaterThan(coverageJson.indexOf("\"handlerKeyCount\""));
        assertThat(coverageJson.indexOf("\"orphanHandlerKeys\""))
                .isGreaterThan(coverageJson.indexOf("\"missingHandlerKeys\""));
    }

    @Test
    void reportsMissingAndOrphanCoverage() {
        WayangA2aJsonRpcHttpDiagnosticHandlerCoverage coverage =
                WayangA2aJsonRpcHttpDiagnosticHandlerCoverage.from(
                        List.of(
                                route(WayangA2aJsonRpcHttpRouteDescriptor.KEY_ENDPOINT),
                                route(WayangA2aJsonRpcHttpRouteDescriptor.KEY_SMOKE),
                                route(WayangA2aJsonRpcHttpRouteDescriptor.KEY_ROUTE_CATALOG)),
                        List.of(WayangA2aJsonRpcHttpRouteDescriptor.KEY_SMOKE, "orphan"));

        assertThat(coverage.complete()).isFalse();
        assertThat(coverage.routeKeys()).containsExactly(
                WayangA2aJsonRpcHttpRouteDescriptor.KEY_SMOKE,
                WayangA2aJsonRpcHttpRouteDescriptor.KEY_ROUTE_CATALOG);
        assertThat(coverage.handlerKeys()).containsExactly(
                WayangA2aJsonRpcHttpRouteDescriptor.KEY_SMOKE,
                "orphan");
        assertThat(coverage.missingHandlerKeys()).containsExactly(
                WayangA2aJsonRpcHttpRouteDescriptor.KEY_ROUTE_CATALOG);
        assertThat(coverage.orphanHandlerKeys()).containsExactly("orphan");
    }

    @Test
    void decodesCoverageMap() {
        WayangA2aJsonRpcHttpDiagnosticHandlerCoverage coverage =
                WayangA2aJsonRpcHttpDiagnosticHandlerCoverage.fromMap(Map.of(
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
    void treatsMissingCoverageMapAsIncomplete() {
        WayangA2aJsonRpcHttpDiagnosticHandlerCoverage coverage =
                WayangA2aJsonRpcHttpDiagnosticHandlerCoverage.fromMap(Map.of());

        assertThat(coverage.complete()).isFalse();
        assertThat(coverage.routeKeyCount()).isZero();
        assertThat(coverage.handlerKeyCount()).isZero();
        assertThat(coverage.routeKeys()).isEmpty();
        assertThat(coverage.handlerKeys()).isEmpty();
        assertThat(coverage.missingHandlerKeys()).isEmpty();
        assertThat(coverage.orphanHandlerKeys()).isEmpty();
        assertThat(coverage.toMap()).containsEntry("complete", false);
    }

    @Test
    void treatsStatusOnlyCoverageMapAsIncomplete() {
        WayangA2aJsonRpcHttpDiagnosticHandlerCoverage coverage =
                WayangA2aJsonRpcHttpDiagnosticHandlerCoverage.fromMap(Map.of("complete", true));

        assertThat(coverage.complete()).isFalse();
        assertThat(coverage.routeKeys()).isEmpty();
        assertThat(coverage.handlerKeys()).isEmpty();
    }

    private static WayangA2aJsonRpcHttpRouteDescriptor route(String key) {
        return WayangA2aJsonRpcHttpRouteDescriptor.fromConfig(WayangA2aJsonRpcHttpConfig.defaults())
                .stream()
                .filter(route -> route.key().equals(key))
                .findFirst()
                .orElseThrow();
    }
}

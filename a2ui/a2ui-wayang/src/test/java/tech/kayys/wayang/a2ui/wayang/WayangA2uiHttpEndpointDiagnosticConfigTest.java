package tech.kayys.wayang.a2ui.wayang;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2uiHttpEndpointDiagnosticConfigTest {

    @Test
    void exposesCanonicalDefaultConfigFactory() {
        assertThat(WayangA2uiHttpEndpointDiagnosticConfig.defaultConfig())
                .isEqualTo(WayangA2uiHttpEndpointDiagnosticConfig.defaults());
        assertThat(WayangA2uiHttpEndpointDiagnosticConfig.fromMap(null))
                .isEqualTo(WayangA2uiHttpEndpointDiagnosticConfig.defaultConfig());
    }

    @Test
    void parsesDiscoveryProfileNestedProbesAndContextMaps() {
        WayangA2uiHttpEndpointDiagnosticConfig config =
                WayangA2uiHttpEndpointDiagnosticConfig.fromMap(Map.of(
                        "profile",
                        "discovery-only",
                        "probes",
                        Map.of(
                                "routeOptions",
                                "true",
                                "smoke",
                                "false"),
                        "defaultHeaders",
                        Map.of(WayangA2uiHttpResponse.HEADER_ACCEPT,
                                List.of(WayangA2uiTransportContent.MIME_JSON)),
                        "defaultAttributes",
                        Map.of(
                                "tenant",
                                "demo",
                                "traceId",
                                "trace-1")));

        assertThat(config.routeCatalogProbe()).isTrue();
        assertThat(config.bindingReportProbe()).isTrue();
        assertThat(config.smokeProbe()).isFalse();
        assertThat(config.readinessProbe()).isFalse();
        assertThat(config.routeOptionsProbe()).isTrue();
        assertThat(config.defaultHeaders())
                .containsEntry(WayangA2uiHttpResponse.HEADER_ACCEPT,
                        List.of(WayangA2uiTransportContent.MIME_JSON));
        assertThat(config.defaultAttributes())
                .containsEntry("tenant", "demo")
                .containsEntry("traceId", "trace-1");
    }

    @Test
    void letsTopLevelProbeValuesOverrideNestedProbeDefaults() {
        WayangA2uiHttpEndpointDiagnosticConfig config =
                WayangA2uiHttpEndpointDiagnosticConfig.fromMap(Map.of(
                        "profile",
                        "default",
                        "probes",
                        Map.of(
                                WayangA2uiHttpEndpointDiagnosticConfig.KEY_SMOKE_PROBE,
                                "false",
                                "readiness",
                                "false"),
                        WayangA2uiHttpEndpointDiagnosticConfig.KEY_SMOKE_PROBE,
                        "true",
                        WayangA2uiHttpEndpointDiagnosticConfig.KEY_ROUTE_CATALOG_PROBE,
                        "false",
                        "headers",
                        Map.of("X-Tenant", "demo"),
                        "attributes",
                        Map.of("source", "quarkus")));

        assertThat(config.routeCatalogProbe()).isFalse();
        assertThat(config.bindingReportProbe()).isTrue();
        assertThat(config.smokeProbe()).isTrue();
        assertThat(config.readinessProbe()).isFalse();
        assertThat(config.routeOptionsProbe()).isTrue();
        assertThat(config.defaultHeaders()).containsEntry("X-Tenant", "demo");
        assertThat(config.defaultAttributes()).containsEntry("source", "quarkus");
        assertThat(config.toMap())
                .containsEntry(WayangA2uiHttpEndpointDiagnosticConfig.KEY_ROUTE_CATALOG_PROBE, false)
                .containsEntry(WayangA2uiHttpEndpointDiagnosticConfig.KEY_SMOKE_PROBE, true)
                .containsEntry(WayangA2uiHttpEndpointDiagnosticConfig.KEY_READINESS_PROBE, false);
    }

    @Test
    void fallsBackToFullDefaultsForBlankOrUnknownProfiles() {
        WayangA2uiHttpEndpointDiagnosticConfig empty =
                WayangA2uiHttpEndpointDiagnosticConfig.fromMap(Map.of());
        WayangA2uiHttpEndpointDiagnosticConfig unknown =
                WayangA2uiHttpEndpointDiagnosticConfig.fromMap(Map.of("profile", "future"));

        assertThat(empty).isEqualTo(WayangA2uiHttpEndpointDiagnosticConfig.defaultConfig());
        assertThat(unknown).isEqualTo(WayangA2uiHttpEndpointDiagnosticConfig.defaultConfig());
    }
}

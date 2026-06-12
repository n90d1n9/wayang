package tech.kayys.wayang.a2ui.wayang;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2uiHttpEndpointDiagnosticConfigDecoderTest {

    @Test
    void decodesNullAndEmptyMapsAsCanonicalDefaultConfig() {
        assertThat(WayangA2uiHttpEndpointDiagnosticConfigDecoder.fromMap(null))
                .isEqualTo(WayangA2uiHttpEndpointDiagnosticConfig.defaultConfig());
        assertThat(WayangA2uiHttpEndpointDiagnosticConfigDecoder.fromMap(Map.of()))
                .isEqualTo(WayangA2uiHttpEndpointDiagnosticConfig.defaultConfig());
    }

    @Test
    void decodesDiscoveryProfileNestedProbesAndContextAliases() {
        WayangA2uiHttpEndpointDiagnosticConfig config =
                WayangA2uiHttpEndpointDiagnosticConfigDecoder.fromMap(Map.of(
                        "profile",
                        "discovery_only",
                        "probes",
                        Map.of(
                                "routeOptions",
                                "true",
                                "smoke",
                                "false"),
                        "headers",
                        Map.of(WayangA2uiHttpResponse.HEADER_ACCEPT,
                                List.of(WayangA2uiTransportContent.MIME_JSON)),
                        "attributes",
                        Map.of("tenant", "demo")));

        assertThat(config.routeCatalogProbe()).isTrue();
        assertThat(config.bindingReportProbe()).isTrue();
        assertThat(config.smokeProbe()).isFalse();
        assertThat(config.readinessProbe()).isFalse();
        assertThat(config.routeOptionsProbe()).isTrue();
        assertThat(config.defaultHeaders())
                .containsEntry(WayangA2uiHttpResponse.HEADER_ACCEPT,
                        List.of(WayangA2uiTransportContent.MIME_JSON));
        assertThat(config.defaultAttributes()).containsEntry("tenant", "demo");
    }

    @Test
    void letsTopLevelProbeValuesOverrideNestedProbeValues() {
        WayangA2uiHttpEndpointDiagnosticConfig config =
                WayangA2uiHttpEndpointDiagnosticConfigDecoder.fromMap(Map.of(
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
                        "false"));

        assertThat(config.routeCatalogProbe()).isFalse();
        assertThat(config.smokeProbe()).isTrue();
        assertThat(config.readinessProbe()).isFalse();
    }

    @Test
    void recordFactoryDelegatesToDecoder() {
        Map<String, Object> values = Map.of(
                "profile",
                "discovery",
                "routeOptionsProbe",
                "true");

        assertThat(WayangA2uiHttpEndpointDiagnosticConfig.fromMap(values))
                .isEqualTo(WayangA2uiHttpEndpointDiagnosticConfigDecoder.fromMap(values));
    }
}

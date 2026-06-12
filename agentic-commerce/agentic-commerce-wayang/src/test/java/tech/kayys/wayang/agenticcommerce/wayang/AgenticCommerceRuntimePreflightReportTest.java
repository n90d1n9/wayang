package tech.kayys.wayang.agenticcommerce.wayang;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgenticCommerceRuntimePreflightReportTest {

    @Test
    void defaultsAreReadyWithDefaultConfigWarnings() {
        AgenticCommerceRuntimePreflightReport report = AgenticCommerceRuntimePreflightReport.from(
                new AgenticCommerceWayangConfigSnapshot(
                        AgenticCommerceWayangRuntimeConfig.defaults(),
                        AgenticCommerceWayangBootstrapConfig.defaults(),
                        false,
                        false,
                        Map.of("storageKind", "file")));
        Map<String, Object> values = report.toMap();
        Map<String, Object> capabilities = map(values.get("persistenceCapabilities"));
        Map<String, Object> persistenceTarget = map(values.get("persistenceTarget"));

        assertThat(report.ready()).isTrue();
        assertThat(report.errors()).isEmpty();
        assertThat(report.warnings())
                .containsExactly("runtime_config_defaulted", "bootstrap_config_defaulted");
        assertThat(values)
                .containsEntry("ready", true)
                .containsEntry("connectorKind", AgenticCommerceConnectorFactoryConfig.CONNECTOR_IN_MEMORY)
                .containsEntry("httpRouteCount", 5)
                .containsKeys(
                        "bindingReport",
                        "runtimeConfig",
                        "bootstrapConfig",
                        "store",
                        "persistenceTarget",
                        "persistenceCapabilities");
        assertThat(persistenceTarget)
                .containsEntry("targetKind", "file")
                .containsEntry("durable", true);
        assertThat(capabilities)
                .containsEntry("storageKind", FileAgenticCommerceWayangPersistenceStore.STORAGE_KIND)
                .containsEntry("durable", true);
    }

    @Test
    void exposesSharedReadinessContractView() {
        AgenticCommerceRuntimePreflightReport report = AgenticCommerceRuntimePreflightReport.from(
                new AgenticCommerceWayangConfigSnapshot(
                        AgenticCommerceWayangRuntimeConfig.defaults(),
                        AgenticCommerceWayangBootstrapConfig.defaults(),
                        false,
                        false,
                        Map.of("storageKind", "file")));
        Map<String, Object> standard = report.standardReadiness().toMap();

        assertThat(standard)
                .containsEntry("readinessId", AgenticCommerceRuntimePreflightReport.READINESS_ID)
                .containsEntry("ready", true)
                .containsEntry("exitCode", 0)
                .containsEntry("issueCount", 0)
                .containsEntry("issues", List.of());
        assertThat(maps(standard.get("probes")))
                .hasSize(5)
                .anySatisfy(probe -> assertThat(probe)
                        .containsEntry("probe", "connectorSupported")
                        .containsEntry("required", true)
                        .containsEntry("passed", true))
                .anySatisfy(probe -> assertThat(probe)
                        .containsEntry("probe", "persistenceStore")
                        .containsEntry("required", false)
                        .containsEntry("passed", true));
        assertThat(map(standard.get("attributes")))
                .containsEntry("warningCount", 2)
                .containsEntry("connectorKind", AgenticCommerceConnectorFactoryConfig.CONNECTOR_IN_MEMORY)
                .containsEntry("connectorSupported", true)
                .containsEntry("httpRouteCount", 5)
                .containsEntry("runtimeConfigSource", "default")
                .containsEntry("bootstrapConfigSource", "default");
    }

    @Test
    void reportsPolicyRejectionForRestrictedConnectorKind() {
        AgenticCommerceRuntimePreflightReport report = AgenticCommerceRuntimePreflightReport.from(
                snapshot(AgenticCommerceWayangRuntimeConfig.builder()
                        .connectorPolicy(AgenticCommerceConnectorPolicy.strictHosted(List.of("seller.example")))
                        .build()));

        assertThat(report.ready()).isFalse();
        assertThat(report.errors()).contains("connector_kind_not_allowed");
        assertThat(report.toMap()).containsEntry("errorCount", 1);
    }

    @Test
    void reportsHttpConnectorConfigAndBindingProblems() {
        AgenticCommerceRuntimePreflightReport report = AgenticCommerceRuntimePreflightReport.from(
                snapshot(AgenticCommerceWayangRuntimeConfig.builder()
                        .connectorFactoryConfig(AgenticCommerceConnectorFactoryConfig.fromMap(Map.of("mode", "http")))
                        .httpConfig(AgenticCommerceHttpAdapterConfig.builder()
                                .smokeEnabled(false)
                                .bindingReportEnabled(false)
                                .build())
                        .build()));

        assertThat(report.ready()).isFalse();
        assertThat(report.errors())
                .contains("connector_base_url_required", "smoke_probe_required_but_disabled");
        assertThat(report.warnings())
                .contains("seller_bearer_token_missing", "binding_report_disabled");
        assertThat(strings(report.toMap().get("connectorPolicyIssues")))
                .containsExactly("connector_base_url_required");
    }

    @Test
    void reportsUnsupportedConnectorKindBeforeBuild() {
        AgenticCommerceRuntimePreflightReport report = AgenticCommerceRuntimePreflightReport.from(
                snapshot(AgenticCommerceWayangRuntimeConfig.builder()
                        .connectorFactoryConfig(AgenticCommerceConnectorFactoryConfig.fromMap(Map.of("mode", "s3")))
                        .build()));

        assertThat(report.ready()).isFalse();
        assertThat(report.errors()).contains("connector_kind_unsupported");
        assertThat(report.toMap()).containsEntry("connectorSupported", false);
    }

    @Test
    void mapsBlockedPreflightErrorsIntoSharedReadinessIssues() {
        AgenticCommerceRuntimePreflightReport report = AgenticCommerceRuntimePreflightReport.from(
                snapshot(AgenticCommerceWayangRuntimeConfig.builder()
                        .connectorFactoryConfig(AgenticCommerceConnectorFactoryConfig.fromMap(Map.of("mode", "s3")))
                        .build()));
        Map<String, Object> standard = report.standardReadiness().toMap();

        assertThat(standard)
                .containsEntry("readinessId", AgenticCommerceRuntimePreflightReport.READINESS_ID)
                .containsEntry("ready", false)
                .containsEntry("exitCode", 1)
                .containsEntry("issueCount", 1);
        assertThat(maps(standard.get("issues")))
                .singleElement()
                .satisfies(issue -> assertThat(issue)
                        .containsEntry("code", "connector_kind_unsupported")
                        .containsEntry("source", "connector")
                        .containsEntry("message", "connector kind unsupported"));
        assertThat(maps(standard.get("probes")))
                .anySatisfy(probe -> assertThat(probe)
                        .containsEntry("probe", "connectorSupported")
                        .containsEntry("passed", false)
                        .containsEntry("issueCount", 1));
        assertThat(map(standard.get("attributes")))
                .containsEntry("connectorKind", "s3")
                .containsEntry("connectorSupported", false);
    }

    private static AgenticCommerceWayangConfigSnapshot snapshot(AgenticCommerceWayangRuntimeConfig runtimeConfig) {
        return new AgenticCommerceWayangConfigSnapshot(
                runtimeConfig,
                AgenticCommerceWayangBootstrapConfig.defaults(),
                true,
                true,
                Map.of("storageKind", "file"));
    }

    @SuppressWarnings("unchecked")
    private static List<String> strings(Object value) {
        assertThat(value).isInstanceOf(List.class);
        return (List<String>) value;
    }

    private static Map<String, Object> map(Object value) {
        assertThat(value).isInstanceOf(Map.class);
        return AgenticCommerceWayangMaps.copy((Map<?, ?>) value);
    }

    private static List<Map<String, Object>> maps(Object value) {
        assertThat(value).isInstanceOf(List.class);
        return ((List<?>) value).stream()
                .map(entry -> {
                    assertThat(entry).isInstanceOf(Map.class);
                    return AgenticCommerceWayangMaps.copy((Map<?, ?>) entry);
                })
                .toList();
    }
}

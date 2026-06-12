package tech.kayys.wayang.agenticcommerce.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceHttpResponse;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgenticCommerceConnectorDiagnosticsTest {

    @Test
    void redactsConnectorSecretsAndExposesReadiness() {
        AgenticCommerceWayangRuntimeConfig runtimeConfig = AgenticCommerceWayangRuntimeConfig.builder()
                .connectorFactoryConfig(AgenticCommerceConnectorFactoryConfig.fromMap(Map.of("mode", "http")))
                .connectorConfig(AgenticCommerceConnectorConfig.bearer("super-secret-token")
                        .withBaseUrl("https://seller.example/shop")
                        .withHeaders(Map.of("X-Seller-Secret", "hidden-header"))
                        .withAttributes(Map.of("tenant", "tenant-demo")))
                .connectorPolicy(AgenticCommerceConnectorPolicy.strictHosted(List.of("seller.example")))
                .build();

        AgenticCommerceConnectorDiagnostics diagnostics = AgenticCommerceConnectorDiagnostics.from(
                snapshot(runtimeConfig));
        Map<String, Object> values = diagnostics.toMap();
        Map<String, Object> auth = map(values.get("auth"));
        Map<String, Object> storage = map(values.get("storage"));
        Map<String, Object> capabilities = map(storage.get("capabilities"));
        Map<String, Object> transport = map(values.get("transport"));
        Map<String, Object> policy = map(values.get("policy"));

        assertThat(diagnostics.ready()).isTrue();
        assertThat(values.toString())
                .doesNotContain("super-secret-token")
                .doesNotContain("hidden-header")
                .doesNotContain("tenant-demo");
        assertThat(auth)
                .containsEntry("bearerTokenConfigured", true)
                .containsEntry("bearerTokenRedacted", true)
                .containsEntry("headerCount", 1)
                .containsEntry("attributeCount", 1);
        assertThat(transport)
                .containsEntry("connectorKind", AgenticCommerceConnectorFactoryConfig.CONNECTOR_HTTP)
                .containsEntry("baseUrlScheme", "https")
                .containsEntry("baseUrlHost", "seller.example")
                .containsEntry("baseUrlValid", true)
                .containsEntry("baseUrlPathConfigured", true);
        assertThat(policy)
                .containsEntry("allowsConnector", true)
                .containsEntry("issues", List.of());
        assertThat(capabilities)
                .containsEntry("storageKind", FileAgenticCommerceWayangPersistenceStore.STORAGE_KIND)
                .containsEntry("durable", true);
        assertThat(map(values.get("preflight"))).containsEntry("ready", true);
    }

    @Test
    void exposesSharedReadinessContractView() {
        AgenticCommerceConnectorDiagnostics diagnostics = AgenticCommerceConnectorDiagnostics.from(
                snapshot(AgenticCommerceWayangRuntimeConfig.defaults()));
        Map<String, Object> standard = diagnostics.standardReadiness().toMap();

        assertThat(standard)
                .containsEntry("readinessId", AgenticCommerceConnectorDiagnostics.READINESS_ID)
                .containsEntry("ready", true)
                .containsEntry("exitCode", 0)
                .containsEntry("issueCount", 0)
                .containsEntry("issues", List.of());
        assertThat(maps(standard.get("probes")))
                .hasSize(2)
                .anySatisfy(probe -> assertThat(probe)
                        .containsEntry("probe", "runtimePreflight")
                        .containsEntry("required", true)
                        .containsEntry("passed", true))
                .anySatisfy(probe -> assertThat(probe)
                        .containsEntry("probe", "connectorContract")
                        .containsEntry("required", false)
                        .containsEntry("passed", true)
                        .containsEntry("issueCount", 0));
        assertThat(map(standard.get("attributes")))
                .containsEntry("connectorKind", AgenticCommerceConnectorFactoryConfig.CONNECTOR_IN_MEMORY)
                .containsEntry("connectorSupported", true)
                .containsEntry("contractAvailable", false);
    }

    @Test
    void includesPolicyAndPreflightIssues() {
        AgenticCommerceWayangRuntimeConfig runtimeConfig = AgenticCommerceWayangRuntimeConfig.builder()
                .connectorFactoryConfig(AgenticCommerceConnectorFactoryConfig.fromMap(Map.of("mode", "http")))
                .connectorConfig(AgenticCommerceConnectorConfig.defaults()
                        .withBaseUrl("http://evil.example"))
                .connectorPolicy(AgenticCommerceConnectorPolicy.strictHosted(List.of("seller.example")))
                .build();

        AgenticCommerceConnectorDiagnostics diagnostics = AgenticCommerceConnectorDiagnostics.from(
                snapshot(runtimeConfig));
        Map<String, Object> values = diagnostics.toMap();
        Map<String, Object> policy = map(values.get("policy"));
        Map<String, Object> preflight = map(values.get("preflight"));

        assertThat(diagnostics.ready()).isFalse();
        assertThat(strings(policy.get("issues")))
                .containsExactly("connector_https_required", "connector_host_not_allowed");
        assertThat(strings(preflight.get("errors")))
                .contains("connector_https_required", "connector_host_not_allowed");
        assertThat(strings(preflight.get("warnings"))).contains("seller_bearer_token_missing");
        assertThat(values).containsEntry("issueCount", 2);
    }

    @Test
    void includesContractSummaryWhenProvided() {
        AgenticCommerceWayangRuntime runtime = AgenticCommerceWayangRuntime.inMemory();
        AgenticCommerceConnectorContractReport contractReport = runtime.connectorContract();

        AgenticCommerceConnectorDiagnostics diagnostics = runtime.connectorDiagnostics(contractReport);
        Map<String, Object> values = diagnostics.toMap();
        Map<String, Object> contract = map(values.get("contract"));
        Map<String, Object> storage = map(values.get("storage"));

        assertThat(diagnostics.contractAvailable()).isTrue();
        assertThat(diagnostics.ready()).isTrue();
        assertThat(contract)
                .containsEntry("passed", true)
                .containsEntry("exchangeCount", 5)
                .containsEntry("issueCount", 0)
                .containsEntry("connectorName", AgenticCommerceConnectorFactoryConfig.CONNECTOR_IN_MEMORY);
        assertThat(storage).containsEntry("storageKind", "runtime");
    }

    @Test
    void failingOptionalContractBlocksSharedReadiness() {
        AgenticCommerceConnector failing = request -> AgenticCommerceHttpResponse.json(500, "{}");
        AgenticCommerceConnectorContractReport contractReport =
                AgenticCommerceConnectorContractHarness.checkoutLifecycle().run(failing);
        AgenticCommerceConnectorDiagnostics diagnostics = AgenticCommerceConnectorDiagnostics.from(
                AgenticCommerceWayangRuntime.inMemory(),
                contractReport);
        Map<String, Object> standard = diagnostics.standardReadiness().toMap();

        assertThat(contractReport.passed()).isFalse();
        assertThat(standard)
                .containsEntry("readinessId", AgenticCommerceConnectorDiagnostics.READINESS_ID)
                .containsEntry("ready", false)
                .containsEntry("exitCode", 1)
                .containsEntry("issueCount", contractReport.issueCount());
        assertThat(maps(standard.get("issues")))
                .singleElement()
                .satisfies(issue -> assertThat(issue)
                        .containsEntry("code", "connector_contract_failed")
                        .containsEntry("source", "contract")
                        .containsEntry("scenarioId", "agentic-commerce-checkout-smoke")
                        .containsEntry("expectationId", "agentic-commerce-checkout-smoke-expectation"));
        assertThat(maps(standard.get("probes")))
                .anySatisfy(probe -> assertThat(probe)
                        .containsEntry("probe", "connectorContract")
                        .containsEntry("passed", false)
                        .containsEntry("issueCount", contractReport.issueCount()));
        assertThat(map(standard.get("attributes")))
                .containsEntry("contractAvailable", true);
    }

    private static AgenticCommerceWayangConfigSnapshot snapshot(AgenticCommerceWayangRuntimeConfig runtimeConfig) {
        return new AgenticCommerceWayangConfigSnapshot(
                runtimeConfig,
                AgenticCommerceWayangBootstrapConfig.defaults(),
                true,
                true,
                Map.of("storageKind", "file"));
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

    @SuppressWarnings("unchecked")
    private static List<String> strings(Object value) {
        assertThat(value).isInstanceOf(List.class);
        return (List<String>) value;
    }
}

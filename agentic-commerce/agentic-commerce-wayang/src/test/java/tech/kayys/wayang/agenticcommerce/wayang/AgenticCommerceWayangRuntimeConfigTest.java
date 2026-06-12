package tech.kayys.wayang.agenticcommerce.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceProtocol;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgenticCommerceWayangRuntimeConfigTest {

    @Test
    void defaultsKeepConnectorAndHttpDefaultsTogether() {
        AgenticCommerceWayangRuntimeConfig config = AgenticCommerceWayangRuntimeConfig.defaults();

        assertThat(config.connectorConfig().apiVersion()).isEqualTo(AgenticCommerceProtocol.SPEC_VERSION);
        assertThat(config.connectorFactoryConfig().connectorKind())
                .isEqualTo(AgenticCommerceConnectorFactoryConfig.CONNECTOR_IN_MEMORY);
        assertThat(config.httpConfig().checkoutBasePath())
                .isEqualTo(AgenticCommerceHttpAdapter.DEFAULT_CHECKOUT_BASE_PATH);
        assertThat(config.connectorPolicy().allowedConnectorKinds()).isEmpty();
        assertThat(config.connectorPolicy().allowedBaseHosts()).isEmpty();
        assertThat(config.toMap())
                .containsEntry("protocol", AgenticCommerceWayang.PROTOCOL_ID)
                .containsKeys("connectorFactoryConfig", "connectorConfig", "httpConfig", "connectorPolicy");
    }

    @Test
    void bindsNestedAndFlatRuntimeConfigValues() {
        AgenticCommerceWayangRuntimeConfig config = AgenticCommerceWayangRuntimeConfig.fromMap(Map.of(
                "seller",
                Map.of(
                        "baseUrl",
                        "https://seller.example/",
                        "bearerToken",
                        "seller-token",
                        "headers",
                        Map.of("X-Seller", "demo")),
                "http",
                Map.of(
                        "basePath",
                        "/commerce/acp",
                        "smokePath",
                        "/internal/acp/smoke",
                        "bindingPath",
                        "/internal/acp/binding"),
                "connectorFactory",
                Map.of(
                        "mode",
                        "seller-http",
                        "metadata",
                        Map.of("tenant", "demo")),
                "connectorPolicy",
                Map.of(
                        "connectors",
                        "seller-http",
                        "hosts",
                        "Seller.Example",
                        "httpsOnly",
                        "true"),
                "apiVersion",
                "2025-09-30"));

        assertThat(config.connectorFactoryConfig().connectorKind())
                .isEqualTo(AgenticCommerceConnectorFactoryConfig.CONNECTOR_HTTP);
        assertThat(config.connectorFactoryConfig().attributes()).containsEntry("tenant", "demo");
        assertThat(config.connectorConfig().baseUrl()).isEqualTo("https://seller.example");
        assertThat(config.connectorConfig().bearerToken()).isEqualTo("seller-token");
        assertThat(config.connectorConfig().apiVersion()).isEqualTo("2025-09-30");
        assertThat(config.connectorConfig().headers()).containsEntry("X-Seller", "demo");
        assertThat(config.httpConfig().checkoutBasePath()).isEqualTo("/commerce/acp");
        assertThat(config.httpConfig().smokePath()).isEqualTo("/internal/acp/smoke");
        assertThat(config.httpConfig().bindingReportPath()).isEqualTo("/internal/acp/binding");
        assertThat(config.connectorPolicy().allowedConnectorKinds())
                .containsExactly(AgenticCommerceConnectorFactoryConfig.CONNECTOR_HTTP);
        assertThat(config.connectorPolicy().allowedBaseHosts()).containsExactly("seller.example");
        assertThat(config.connectorPolicy().requireHttps()).isTrue();
    }

    @Test
    void buildsInMemoryRuntimeFromUnifiedConfig() {
        AgenticCommerceWayangRuntimeConfig config = AgenticCommerceWayangRuntimeConfig.builder()
                .connectorConfig(AgenticCommerceConnectorConfig.bearer("seller-token")
                        .withAttributes(Map.of("tenant", "demo")))
                .connectorPolicy(AgenticCommerceConnectorPolicy.strictHosted(List.of("seller.example")))
                .httpConfig(AgenticCommerceHttpAdapterConfig.builder()
                        .checkoutBasePath("/commerce/acp")
                        .smokePath("/internal/acp/smoke")
                        .bindingReportPath("/internal/acp/binding")
                        .build())
                .build();

        AgenticCommerceWayangRuntime runtime = config.buildInMemory();

        assertThat(runtime.connectorConfig().bearerToken()).isEqualTo("seller-token");
        assertThat(runtime.connectorConfig().attributes()).containsEntry("tenant", "demo");
        assertThat(runtime.httpConfig().checkoutBasePath()).isEqualTo("/commerce/acp");
        assertThat(runtime.connectorPolicy().allowedBaseHosts()).containsExactly("seller.example");
        assertThat(runtime.runtimeConfig().toMap())
                .containsKeys("connectorFactoryConfig", "connectorConfig", "httpConfig", "connectorPolicy");
    }

    @Test
    void buildsConfiguredConnectorFromPersistedFactoryConfig() {
        AgenticCommerceWayangRuntimeConfig config = AgenticCommerceWayangRuntimeConfig.builder()
                .connectorFactoryConfig(AgenticCommerceConnectorFactoryConfig.fromMap(Map.of("mode", "seller-http")))
                .connectorConfig(AgenticCommerceConnectorConfig.defaults()
                        .withBaseUrl("https://seller.example"))
                .connectorPolicy(AgenticCommerceConnectorPolicy.strictHosted(List.of("seller.example")))
                .build();

        AgenticCommerceWayangRuntime runtime = config.build();

        assertThat(runtime.connector()).isInstanceOf(HttpAgenticCommerceConnector.class);
        assertThat(runtime.connectorFactoryConfig().connectorKind())
                .isEqualTo(AgenticCommerceConnectorFactoryConfig.CONNECTOR_HTTP);
        assertThat(runtime.runtimeConfig().connectorFactoryConfig().connectorKind())
                .isEqualTo(AgenticCommerceConnectorFactoryConfig.CONNECTOR_HTTP);
    }
}

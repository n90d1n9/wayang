package tech.kayys.wayang.agenticcommerce.wayang;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgenticCommerceConnectorFactoryConfigTest {

    @Test
    void defaultsBuildInMemoryConnector() {
        AgenticCommerceConnectorFactoryConfig config = AgenticCommerceConnectorFactoryConfig.defaults();
        AgenticCommerceConnector connector = config.buildConnector(AgenticCommerceConnectorConfig.defaults());

        assertThat(config.connectorKind()).isEqualTo(AgenticCommerceConnectorFactoryConfig.CONNECTOR_IN_MEMORY);
        assertThat(config.inMemoryConnector()).isTrue();
        assertThat(connector).isInstanceOf(InMemoryAgenticCommerceConnector.class);
        assertThat(config.toMap())
                .containsEntry("connectorKind", AgenticCommerceConnectorFactoryConfig.CONNECTOR_IN_MEMORY)
                .containsEntry("inMemoryConnector", true)
                .containsEntry("attributeCount", 0);
        assertThat(config.toStorageMap())
                .containsEntry("connectorKind", AgenticCommerceConnectorFactoryConfig.CONNECTOR_IN_MEMORY)
                .containsEntry("attributes", Map.of())
                .doesNotContainKey("inMemoryConnector");
    }

    @Test
    void bindsAliasesAndAttributesFromMap() {
        AgenticCommerceConnectorFactoryConfig config = AgenticCommerceConnectorFactoryConfig.fromMap(Map.of(
                "mode",
                "memory",
                "metadata",
                Map.of("tenant", "demo")));

        assertThat(config.connectorKind()).isEqualTo(AgenticCommerceConnectorFactoryConfig.CONNECTOR_IN_MEMORY);
        assertThat(config.attributes()).containsEntry("tenant", "demo");
        assertThat(config.toMap()).containsEntry("attributeCount", 1);
    }

    @Test
    void httpAliasesBuildHttpConnectorWhenBaseUrlIsConfigured() {
        AgenticCommerceConnectorFactoryConfig config = AgenticCommerceConnectorFactoryConfig.fromMap(Map.of(
                "connectorKind",
                "seller-http"));
        AgenticCommerceConnector connector = config.buildConnector(
                AgenticCommerceConnectorConfig.defaults().withBaseUrl("http://localhost:1234"));

        assertThat(config.connectorKind()).isEqualTo(AgenticCommerceConnectorFactoryConfig.CONNECTOR_HTTP);
        assertThat(connector).isInstanceOf(HttpAgenticCommerceConnector.class);
        assertThat(config.toMap()).containsEntry("httpConnector", true);
    }

    @Test
    void httpConnectorRequiresBaseUrl() {
        AgenticCommerceConnectorFactoryConfig config = AgenticCommerceConnectorFactoryConfig.fromMap(Map.of(
                "connectorKind",
                "http"));

        assertThatThrownBy(() -> config.buildConnector(AgenticCommerceConnectorConfig.defaults()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("connector_base_url_required");
    }

    @Test
    void unsupportedConnectorKindsFailExplicitlyAtBuildTime() {
        AgenticCommerceConnectorFactoryConfig config = AgenticCommerceConnectorFactoryConfig.fromMap(Map.of(
                "connectorKind",
                "s3"));

        assertThat(config.connectorKind()).isEqualTo("s3");
        assertThat(config.inMemoryConnector()).isFalse();
        assertThatThrownBy(() -> config.buildConnector(AgenticCommerceConnectorConfig.defaults()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported Agentic Commerce connector kind: s3");
    }
}

package tech.kayys.wayang.agenticcommerce.wayang;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgenticCommerceConnectorPolicyTest {

    @Test
    void defaultsAllowConfiguredHttpAndInMemoryConnectors() {
        AgenticCommerceConnectorPolicy policy = AgenticCommerceConnectorPolicy.defaults();

        assertThat(policy.allows(
                AgenticCommerceConnectorFactoryConfig.inMemory(),
                AgenticCommerceConnectorConfig.defaults()))
                .isTrue();
        assertThat(policy.allows(
                AgenticCommerceConnectorFactoryConfig.fromMap(Map.of("mode", "http")),
                AgenticCommerceConnectorConfig.defaults().withBaseUrl("http://seller.example")))
                .isTrue();
        assertThat(policy.toMap())
                .containsEntry("connectorKindRestricted", false)
                .containsEntry("baseHostRestricted", false);
    }

    @Test
    void strictHostedRequiresHttpsAndAllowedHost() {
        AgenticCommerceConnectorPolicy policy = AgenticCommerceConnectorPolicy.strictHosted(List.of("seller.example"));
        AgenticCommerceConnectorFactoryConfig factory =
                AgenticCommerceConnectorFactoryConfig.fromMap(Map.of("mode", "seller-http"));
        AgenticCommerceConnectorConfig allowed =
                AgenticCommerceConnectorConfig.defaults().withBaseUrl("https://seller.example");
        AgenticCommerceConnectorConfig disallowedScheme =
                AgenticCommerceConnectorConfig.defaults().withBaseUrl("http://seller.example");
        AgenticCommerceConnectorConfig disallowedHost =
                AgenticCommerceConnectorConfig.defaults().withBaseUrl("https://evil.example");

        assertThat(policy.allows(factory, allowed)).isTrue();
        assertThat(policy.issues(factory, disallowedScheme)).contains("connector_https_required");
        assertThat(policy.issues(factory, disallowedHost)).contains("connector_host_not_allowed");
        assertThatThrownBy(() -> factory.buildConnector(disallowedHost, policy))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("connector_host_not_allowed");
    }

    @Test
    void fromMapNormalizesConnectorAliasesAndHosts() {
        AgenticCommerceConnectorPolicy policy = AgenticCommerceConnectorPolicy.fromMap(Map.of(
                "connectors",
                "seller-http memory",
                "hosts",
                List.of("Seller.Example", "SELLER.example"),
                "httpsOnly",
                "true"));

        assertThat(policy.allowedConnectorKinds())
                .containsExactly(
                        AgenticCommerceConnectorFactoryConfig.CONNECTOR_HTTP,
                        AgenticCommerceConnectorFactoryConfig.CONNECTOR_IN_MEMORY);
        assertThat(policy.allowedBaseHosts()).containsExactly("seller.example");
        assertThat(policy.requireHttps()).isTrue();
    }

    @Test
    void disallowsConnectorKindWhenRestricted() {
        AgenticCommerceConnectorPolicy policy = AgenticCommerceConnectorPolicy.fromMap(Map.of(
                "allowedConnectorKinds",
                List.of("in-memory")));
        AgenticCommerceConnectorFactoryConfig factory =
                AgenticCommerceConnectorFactoryConfig.fromMap(Map.of("mode", "http"));

        assertThat(policy.issues(
                factory,
                AgenticCommerceConnectorConfig.defaults().withBaseUrl("https://seller.example")))
                .contains("connector_kind_not_allowed");
    }
}

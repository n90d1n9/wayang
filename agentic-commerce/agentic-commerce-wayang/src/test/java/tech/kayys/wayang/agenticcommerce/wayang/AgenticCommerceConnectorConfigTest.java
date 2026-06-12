package tech.kayys.wayang.agenticcommerce.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceCheckoutHttpRequests;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceHttpRequest;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceProtocol;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgenticCommerceConnectorConfigTest {

    @Test
    void defaultsUseCurrentSpecVersion() {
        AgenticCommerceConnectorConfig config = AgenticCommerceConnectorConfig.defaults();

        assertThat(config.apiVersion()).isEqualTo(AgenticCommerceProtocol.SPEC_VERSION);
        assertThat(config.baseUrl()).isBlank();
        assertThat(config.requestOptions().apiVersion()).isEqualTo(AgenticCommerceProtocol.SPEC_VERSION);
    }

    @Test
    void bearerAndExtraHeadersBecomeRequestOptions() {
        AgenticCommerceConnectorConfig config = AgenticCommerceConnectorConfig.bearer("seller-token")
                .withBaseUrl("https://seller.example/")
                .withHeaders(Map.of("X-Seller", "demo"))
                .withAttributes(Map.of("tenant", "acme"));

        AgenticCommerceHttpRequest request = AgenticCommerceCheckoutHttpRequests.retrieve(
                "cs_1",
                config.requestOptions());

        assertThat(config.baseUrl()).isEqualTo("https://seller.example");
        assertThat(request.authorization()).isEqualTo("Bearer seller-token");
        assertThat(request.apiVersion()).isEqualTo(AgenticCommerceProtocol.SPEC_VERSION);
        assertThat(request.header("X-Seller")).contains("demo");
        assertThat(request.attributes()).containsEntry("tenant", "acme");
    }

    @Test
    void bindsFromMapWithAliasesAndRedactsSecretsInDiagnostics() {
        AgenticCommerceConnectorConfig config = AgenticCommerceConnectorConfig.fromMap(Map.of(
                "sellerBaseUrl",
                "https://seller.example/",
                "token",
                "seller-token",
                "specVersion",
                "2025-09-30",
                "sellerHeaders",
                Map.of("X-Seller", "demo"),
                "metadata",
                Map.of("tenant", "acme")));

        assertThat(config.baseUrl()).isEqualTo("https://seller.example");
        assertThat(config.bearerToken()).isEqualTo("seller-token");
        assertThat(config.apiVersion()).isEqualTo("2025-09-30");
        assertThat(config.headers()).containsEntry("X-Seller", "demo");
        assertThat(config.attributes()).containsEntry("tenant", "acme");
        assertThat(config.toMap())
                .containsEntry("baseUrl", "https://seller.example")
                .containsEntry("apiVersion", "2025-09-30")
                .containsEntry("bearerTokenConfigured", true)
                .containsEntry("headerCount", 1)
                .containsEntry("attributeCount", 1)
                .doesNotContainKey("bearerToken");
    }
}

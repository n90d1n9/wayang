package tech.kayys.wayang.agenticcommerce.wayang;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgenticCommerceWayangRuntimeProfileTest {

    @Test
    void localProfileBuildsReadyInMemoryRuntime() {
        AgenticCommerceWayangRuntimeProfile profile = AgenticCommerceWayangRuntimeProfile.local();
        AgenticCommerceRuntimePreflightReport preflight = profile.preflight();
        AgenticCommerceWayangRuntime runtime = profile.runtime();

        assertThat(profile.profileName()).isEqualTo(AgenticCommerceWayangRuntimeProfile.PROFILE_LOCAL);
        assertThat(profile.runtimeConfig().connectorFactoryConfig().connectorKind())
                .isEqualTo(AgenticCommerceConnectorFactoryConfig.CONNECTOR_IN_MEMORY);
        assertThat(preflight.ready()).isTrue();
        assertThat(preflight.warnings()).isEmpty();
        assertThat(runtime.connector()).isInstanceOf(InMemoryAgenticCommerceConnector.class);
        assertThat(profile.toMap()).containsKeys("profileName", "runtimeConfig", "bootstrapConfig", "preflight");
    }

    @Test
    void sellerHttpProfileBuildsHttpRuntimeWithPermissivePolicy() {
        AgenticCommerceWayangRuntimeProfile profile =
                AgenticCommerceWayangRuntimeProfile.sellerHttp("http://seller.example/");
        AgenticCommerceWayangRuntime runtime = profile.runtime();

        assertThat(profile.profileName()).isEqualTo(AgenticCommerceWayangRuntimeProfile.PROFILE_SELLER_HTTP);
        assertThat(profile.runtimeConfig().connectorFactoryConfig().connectorKind())
                .isEqualTo(AgenticCommerceConnectorFactoryConfig.CONNECTOR_HTTP);
        assertThat(profile.runtimeConfig().connectorConfig().baseUrl()).isEqualTo("http://seller.example");
        assertThat(profile.runtimeConfig().connectorPolicy().requireHttps()).isFalse();
        assertThat(profile.preflight().ready()).isTrue();
        assertThat(profile.preflight().warnings()).contains("seller_bearer_token_missing");
        assertThat(runtime.connector()).isInstanceOf(HttpAgenticCommerceConnector.class);
    }

    @Test
    void productionProfileRequiresHttpsAndInferredHost() {
        AgenticCommerceWayangRuntimeProfile profile =
                AgenticCommerceWayangRuntimeProfile.production("https://seller.example/");
        AgenticCommerceWayangRuntimeProfile insecure =
                AgenticCommerceWayangRuntimeProfile.production("http://seller.example/");

        assertThat(profile.profileName()).isEqualTo(AgenticCommerceWayangRuntimeProfile.PROFILE_PRODUCTION);
        assertThat(profile.runtimeConfig().connectorPolicy().requireHttps()).isTrue();
        assertThat(profile.runtimeConfig().connectorPolicy().allowedBaseHosts()).containsExactly("seller.example");
        assertThat(profile.preflight().ready()).isTrue();
        assertThat(insecure.preflight().ready()).isFalse();
        assertThat(insecure.preflight().errors()).contains("connector_https_required");
    }

    @Test
    void fromMapAppliesProfileDefaultsAndNestedOverrides() {
        AgenticCommerceWayangRuntimeProfile profile = AgenticCommerceWayangRuntimeProfile.fromMap(Map.of(
                "profile",
                "production",
                "seller",
                Map.of(
                        "baseUrl",
                        "https://seller.example/",
                        "bearerToken",
                        "seller-token"),
                "bootstrap",
                Map.of(
                        "includeRuntimeSkills",
                        false,
                        "requireSkillRegistration",
                        false),
                "profileAttributes",
                Map.of("owner", "commerce")));

        assertThat(profile.profileName()).isEqualTo(AgenticCommerceWayangRuntimeProfile.PROFILE_PRODUCTION);
        assertThat(profile.runtimeConfig().connectorFactoryConfig().connectorKind())
                .isEqualTo(AgenticCommerceConnectorFactoryConfig.CONNECTOR_HTTP);
        assertThat(profile.runtimeConfig().connectorConfig().baseUrl()).isEqualTo("https://seller.example");
        assertThat(profile.runtimeConfig().connectorConfig().bearerToken()).isEqualTo("seller-token");
        assertThat(profile.runtimeConfig().connectorPolicy().allowedBaseHosts()).containsExactly("seller.example");
        assertThat(profile.bootstrapConfig().includeRuntimeSkills()).isFalse();
        assertThat(profile.bootstrapConfig().requireSkillRegistration()).isFalse();
        assertThat(profile.attributes()).containsEntry("owner", "commerce");
        assertThat(profile.toStorageMap()).containsKeys("profileName", "runtimeConfig", "bootstrapConfig", "attributes");
    }
}

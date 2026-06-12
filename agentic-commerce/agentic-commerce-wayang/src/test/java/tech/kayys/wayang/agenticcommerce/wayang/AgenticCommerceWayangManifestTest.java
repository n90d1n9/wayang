package tech.kayys.wayang.agenticcommerce.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceJson;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceProtocol;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgenticCommerceWayangManifestTest {

    @Test
    void defaultsExposeCheckoutDiscoverySurface() {
        AgenticCommerceWayangManifest manifest = AgenticCommerceWayangManifest.defaults();
        Map<String, Object> values = manifest.toMap();

        assertThat(manifest.skillCount()).isEqualTo(5);
        assertThat(manifest.routeCount()).isEqualTo(5);
        assertThat(manifest.skillIds()).containsExactlyElementsOf(AgenticCommerceWayang.checkoutSkillIds());
        assertThat(manifest.operations()).containsExactly(
                AgenticCommerceProtocol.OPERATION_CREATE_CHECKOUT_SESSION,
                AgenticCommerceProtocol.OPERATION_RETRIEVE_CHECKOUT_SESSION,
                AgenticCommerceProtocol.OPERATION_UPDATE_CHECKOUT_SESSION,
                AgenticCommerceProtocol.OPERATION_COMPLETE_CHECKOUT_SESSION,
                AgenticCommerceProtocol.OPERATION_CANCEL_CHECKOUT_SESSION);
        assertThat(values)
                .containsEntry("protocol", AgenticCommerceWayang.PROTOCOL_ID)
                .containsEntry("specVersion", AgenticCommerceProtocol.SPEC_VERSION)
                .containsEntry("skillCount", 5)
                .containsEntry("routeCount", 5)
                .containsKeys("runtimeConfig", "bootstrapConfig", "bindingReport", "checkoutSkills");
        assertThat(maps(values.get("checkoutSkills"))).hasSize(5);
        assertThat(maps(values.get("checkoutSkills")).get(0))
                .containsEntry("id", AgenticCommerceWayang.SKILL_CREATE_CHECKOUT)
                .containsEntry("operation", AgenticCommerceProtocol.OPERATION_CREATE_CHECKOUT_SESSION)
                .containsEntry("httpMethod", "POST")
                .containsEntry("pathTemplate", AgenticCommerceProtocol.PATH_CHECKOUT_SESSIONS)
                .containsEntry("outputFormat", "json");
    }

    @Test
    void toJsonProducesParseableDiscoveryDocument() {
        Map<String, Object> values = AgenticCommerceJson.readObject(AgenticCommerceWayangManifest.defaults().toJson());

        assertThat(values).containsEntry("protocol", AgenticCommerceWayang.PROTOCOL_ID);
        assertThat(((Number) values.get("skillCount")).intValue()).isEqualTo(5);
        assertThat(((Number) values.get("routeCount")).intValue()).isEqualTo(5);
        assertThat(maps(values.get("checkoutSkills"))).hasSize(5);
    }

    @Test
    void runtimeManifestCarriesConfiguredRuntimeAndBootstrapPolicy() {
        AgenticCommerceWayangRuntime runtime = AgenticCommerceWayangRuntime.inMemory(
                AgenticCommerceWayangRuntimeConfig.builder()
                        .connectorConfig(AgenticCommerceConnectorConfig.bearer("seller-token")
                                .withAttributes(Map.of("tenant", "demo")))
                        .httpConfig(AgenticCommerceHttpAdapterConfig.builder()
                                .checkoutBasePath("/commerce/acp")
                                .smokePath("/internal/acp/smoke")
                                .bindingReportPath("/internal/acp/binding")
                                .build())
                        .build());
        AgenticCommerceWayangManifest manifest = runtime.manifest(AgenticCommerceWayangBootstrapConfig.builder()
                .skillIds(List.of(AgenticCommerceWayang.SKILL_CREATE_CHECKOUT))
                .includeRuntimeSkills(false)
                .requireSkillRegistration(false)
                .build());
        Map<String, Object> values = manifest.toMap();
        Map<String, Object> runtimeConfig = map(values.get("runtimeConfig"));
        Map<String, Object> httpConfig = map(runtimeConfig.get("httpConfig"));
        Map<String, Object> bootstrapConfig = map(values.get("bootstrapConfig"));
        Map<String, Object> bindingReport = map(values.get("bindingReport"));
        Map<String, Object> bindingConfig = map(bindingReport.get("config"));

        assertThat(httpConfig).containsEntry("checkoutBasePath", "/commerce/acp");
        assertThat(bindingConfig).containsEntry("bindingReportPath", "/internal/acp/binding");
        assertThat(bootstrapConfig)
                .containsEntry("includeRuntimeSkills", false)
                .containsEntry("requireSkillRegistration", false)
                .containsEntry("skillIds", List.of(AgenticCommerceWayang.SKILL_CREATE_CHECKOUT));
    }

    private static Map<String, Object> map(Object value) {
        assertThat(value).isInstanceOf(Map.class);
        return AgenticCommerceWayangMaps.copy((Map<?, ?>) value);
    }

    private static List<Map<String, Object>> maps(Object value) {
        assertThat(value).isInstanceOf(List.class);
        return ((List<?>) value).stream()
                .map(item -> {
                    assertThat(item).isInstanceOf(Map.class);
                    return AgenticCommerceWayangMaps.copy((Map<?, ?>) item);
                })
                .toList();
    }
}

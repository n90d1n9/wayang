package tech.kayys.wayang.agenticcommerce.wayang;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryAgenticCommerceWayangPersistenceStoreTest {

    @Test
    void emptyStoreReportsNoDocuments() {
        InMemoryAgenticCommerceWayangPersistenceStore store =
                InMemoryAgenticCommerceWayangPersistenceStore.create();

        assertThat(store.loadRuntimeConfig()).isEmpty();
        assertThat(store.loadBootstrapConfig()).isEmpty();
        assertThat(store.loadBootstrapReport()).isEmpty();
        assertThat(store.loadManifest()).isEmpty();
        assertThat(store.toMap())
                .containsEntry("storageKind", InMemoryAgenticCommerceWayangPersistenceStore.STORAGE_KIND)
                .containsEntry("ephemeral", true)
                .containsEntry("documentCount", 0)
                .containsEntry("runtimeConfigAvailable", false)
                .containsEntry("manifestAvailable", false);
    }

    @Test
    void runtimeAndBootstrapConfigRoundTripThroughMemorySnapshot() {
        InMemoryAgenticCommerceWayangPersistenceStore store =
                InMemoryAgenticCommerceWayangPersistenceStore.create();
        AgenticCommerceWayangRuntimeConfig runtimeConfig = AgenticCommerceWayangRuntimeConfig.builder()
                .connectorFactoryConfig(AgenticCommerceConnectorFactoryConfig.fromMap(Map.of("mode", "seller-http")))
                .connectorConfig(AgenticCommerceConnectorConfig.bearer("seller-secret")
                        .withBaseUrl("https://seller.example")
                        .withHeaders(Map.of("X-Seller", "demo"))
                        .withAttributes(Map.of("tenant", "acme")))
                .connectorPolicy(AgenticCommerceConnectorPolicy.strictHosted(List.of("seller.example")))
                .httpConfig(AgenticCommerceHttpAdapterConfig.builder()
                        .checkoutBasePath("/commerce/acp")
                        .smokePath("/internal/acp/smoke")
                        .bindingReportPath("/internal/acp/binding")
                        .build())
                .build();
        AgenticCommerceWayangBootstrapConfig bootstrapConfig = AgenticCommerceWayangBootstrapConfig.builder()
                .skillIds(List.of(AgenticCommerceWayang.SKILL_CREATE_CHECKOUT))
                .includeRuntimeSkills(false)
                .requireSkillRegistration(false)
                .build();

        store.saveRuntimeConfig(runtimeConfig);
        store.saveBootstrapConfig(bootstrapConfig);

        AgenticCommerceWayangRuntimeConfig loadedRuntimeConfig = store.loadRuntimeConfig().orElseThrow();
        AgenticCommerceWayangBootstrapConfig loadedBootstrapConfig = store.loadBootstrapConfig().orElseThrow();
        assertThat(loadedRuntimeConfig.connectorFactoryConfig().connectorKind())
                .isEqualTo(AgenticCommerceConnectorFactoryConfig.CONNECTOR_HTTP);
        assertThat(loadedRuntimeConfig.connectorConfig().baseUrl()).isEqualTo("https://seller.example");
        assertThat(loadedRuntimeConfig.connectorConfig().bearerToken()).isEqualTo("seller-secret");
        assertThat(loadedRuntimeConfig.connectorConfig().headers()).containsEntry("X-Seller", "demo");
        assertThat(loadedRuntimeConfig.connectorPolicy().allowedBaseHosts()).containsExactly("seller.example");
        assertThat(loadedBootstrapConfig.skillIds()).containsExactly(AgenticCommerceWayang.SKILL_CREATE_CHECKOUT);
        assertThat(loadedBootstrapConfig.includeRuntimeSkills()).isFalse();
        assertThat(store.toMap()).containsEntry("documentCount", 2);
    }

    @Test
    void persistsBootstrapReportAndManifestSnapshots() {
        InMemoryAgenticCommerceWayangPersistenceStore store =
                InMemoryAgenticCommerceWayangPersistenceStore.create();
        AgenticCommerceWayangRuntime runtime = AgenticCommerceWayangRuntime.inMemory();
        AgenticCommerceWayangBootstrapConfig bootstrapConfig = AgenticCommerceWayangBootstrapConfig.builder()
                .skillIds(List.of(AgenticCommerceWayang.SKILL_CREATE_CHECKOUT))
                .includeRuntimeSkills(false)
                .build();
        AgenticCommerceWayangBootstrapReport report =
                runtime.bootstrap(new AgenticCommerceTestSkillRegistry(), bootstrapConfig);
        AgenticCommerceWayangManifest manifest = runtime.manifest(bootstrapConfig);

        store.saveBootstrapReport(report);
        store.saveManifest(manifest);

        Map<String, Object> loadedReport = store.loadBootstrapReport().orElseThrow();
        Map<String, Object> loadedManifest = store.loadManifest().orElseThrow();
        Map<String, Object> reportRegistration = map(loadedReport.get("skillRegistration"));

        assertThat(loadedReport).containsEntry("ready", true);
        assertThat(number(reportRegistration.get("definitionCount"))).isEqualTo(1);
        assertThat(number(reportRegistration.get("runtimeSkillCount"))).isZero();
        assertThat(number(loadedManifest.get("skillCount"))).isEqualTo(5);
        assertThat(store.toMap())
                .containsEntry("bootstrapReportAvailable", true)
                .containsEntry("manifestAvailable", true)
                .containsEntry("documentCount", 2);
    }

    @Test
    void clearRemovesAllDocuments() {
        InMemoryAgenticCommerceWayangPersistenceStore store =
                InMemoryAgenticCommerceWayangPersistenceStore.create();
        store.saveRuntimeConfig(AgenticCommerceWayangRuntimeConfig.defaults());
        store.saveBootstrapConfig(AgenticCommerceWayangBootstrapConfig.defaults());

        store.clear();

        assertThat(store.loadRuntimeConfig()).isEmpty();
        assertThat(store.loadBootstrapConfig()).isEmpty();
        assertThat(store.toMap()).containsEntry("documentCount", 0);
    }

    private static Map<String, Object> map(Object value) {
        assertThat(value).isInstanceOf(Map.class);
        return AgenticCommerceWayangMaps.copy((Map<?, ?>) value);
    }

    private static int number(Object value) {
        assertThat(value).isInstanceOf(Number.class);
        return ((Number) value).intValue();
    }
}

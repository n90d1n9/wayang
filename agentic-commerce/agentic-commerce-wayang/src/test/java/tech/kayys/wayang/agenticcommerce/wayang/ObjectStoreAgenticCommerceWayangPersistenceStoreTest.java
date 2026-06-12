package tech.kayys.wayang.agenticcommerce.wayang;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ObjectStoreAgenticCommerceWayangPersistenceStoreTest {

    @Test
    void savesAndLoadsPersistenceDocumentsThroughObjectKeys() {
        InMemoryAgenticCommerceObjectStoreClient client = InMemoryAgenticCommerceObjectStoreClient.create();
        AgenticCommerceObjectStoreConfig config = AgenticCommerceObjectStoreConfig.fromMap(Map.of(
                "provider",
                "s3",
                "endpoint",
                "https://s3.example/",
                "bucket",
                "wayang-state",
                "keyPrefix",
                "/prod/agentic-commerce/"));
        ObjectStoreAgenticCommerceWayangPersistenceStore store =
                ObjectStoreAgenticCommerceWayangPersistenceStore.configured(config, client);
        AgenticCommerceWayangRuntime runtime = AgenticCommerceWayangRuntime.inMemory(runtimeConfig());

        store.saveRuntimeConfig(runtime.runtimeConfig());
        store.saveBootstrapConfig(AgenticCommerceWayangBootstrapConfig.runtimeSkillsOnly());
        store.saveBootstrapReport(runtime.bootstrap(
                new AgenticCommerceTestSkillRegistry(),
                AgenticCommerceWayangBootstrapConfig.runtimeSkillsOnly()));
        store.saveManifest(runtime.manifest());

        assertThat(store.loadRuntimeConfig().orElseThrow().connectorConfig().bearerToken())
                .isEqualTo("seller-token");
        assertThat(store.loadBootstrapConfig().orElseThrow().includeDefinitions()).isFalse();
        assertThat(store.loadBootstrapReport()).isPresent();
        assertThat(store.loadManifest()).isPresent();
        assertThat(client.contains("wayang-state", "prod/agentic-commerce/runtime-config.json")).isTrue();
        assertThat(client.contains("wayang-state", "prod/agentic-commerce/bootstrap-config.json")).isTrue();
        assertThat(store.toMap())
                .containsEntry("storageKind", ObjectStoreAgenticCommerceWayangPersistenceStore.STORAGE_KIND)
                .containsEntry("runtimeConfigKey", "prod/agentic-commerce/runtime-config.json")
                .containsEntry("availabilityChecked", false);
        assertThat(map(store.toMap().get("objectStore")))
                .containsEntry("provider", AgenticCommerceObjectStoreConfig.PROVIDER_S3)
                .containsEntry("endpoint", "https://s3.example")
                .containsEntry("bucket", "wayang-state")
                .containsEntry("keyPrefix", "prod/agentic-commerce");
        assertThat(map(store.toMap().get("target")))
                .containsEntry("targetKind", "object-store")
                .containsEntry("provider", AgenticCommerceObjectStoreConfig.PROVIDER_S3)
                .containsEntry("location", "wayang-state/prod/agentic-commerce")
                .containsEntry("cloudStorage", true)
                .containsEntry("durable", true);
    }

    @Test
    void missingObjectsReturnEmptyOptionals() {
        ObjectStoreAgenticCommerceWayangPersistenceStore store =
                ObjectStoreAgenticCommerceWayangPersistenceStore.configured(
                        AgenticCommerceObjectStoreConfig.fromMap(Map.of(
                                "provider",
                                "rustfs",
                                "bucket",
                                "wayang-state",
                                "keyPrefix",
                                "dev")),
                        InMemoryAgenticCommerceObjectStoreClient.create());

        assertThat(store.loadRuntimeConfig()).isEmpty();
        assertThat(store.loadBootstrapConfig()).isEmpty();
        assertThat(store.loadBootstrapReport()).isEmpty();
        assertThat(store.loadManifest()).isEmpty();
        assertThat(store.runtimeConfigKey()).isEqualTo("dev/runtime-config.json");
    }

    private static AgenticCommerceWayangRuntimeConfig runtimeConfig() {
        return AgenticCommerceWayangRuntimeConfig.builder()
                .connectorFactoryConfig(AgenticCommerceConnectorFactoryConfig.fromMap(Map.of("mode", "http")))
                .connectorConfig(AgenticCommerceConnectorConfig.bearer("seller-token")
                        .withBaseUrl("https://seller.example"))
                .connectorPolicy(AgenticCommerceConnectorPolicy.strictHosted(List.of("seller.example")))
                .build();
    }

    private static Map<String, Object> map(Object value) {
        assertThat(value).isInstanceOf(Map.class);
        return AgenticCommerceWayangMaps.copy((Map<?, ?>) value);
    }
}

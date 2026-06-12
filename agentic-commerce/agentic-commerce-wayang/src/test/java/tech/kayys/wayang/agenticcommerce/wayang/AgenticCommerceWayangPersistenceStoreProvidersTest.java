package tech.kayys.wayang.agenticcommerce.wayang;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgenticCommerceWayangPersistenceStoreProvidersTest {

    @Test
    void defaultsExposeBuiltInProviders() {
        AgenticCommerceWayangPersistenceStoreProviders providers =
                AgenticCommerceWayangPersistenceStoreProviders.defaults();

        assertThat(providers.storageKinds())
                .containsExactly(
                        AgenticCommerceWayangPersistenceConfig.STORAGE_FILE,
                        AgenticCommerceWayangPersistenceConfig.STORAGE_IN_MEMORY,
                        AgenticCommerceWayangPersistenceConfig.STORAGE_HYBRID,
                        AgenticCommerceWayangPersistenceConfig.STORAGE_OBJECT_STORE,
                        AgenticCommerceWayangPersistenceConfig.STORAGE_DATABASE);
        assertThat(providers.toMap())
                .containsEntry("providerCount", 5)
                .containsKey("providers");
    }

    @Test
    void objectStoreProviderStillRequiresClientResolver() {
        AgenticCommerceWayangPersistenceConfig config = AgenticCommerceWayangPersistenceConfig.fromMap(Map.of(
                "storageKind",
                "s3",
                "bucket",
                "wayang-state"));

        assertThatThrownBy(() -> AgenticCommerceWayangPersistenceStoreProviders.defaults().build(config))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("requires an AgenticCommerceObjectStoreClient");
    }

    @Test
    void customProviderCanBuildFutureStorageKind() {
        AgenticCommerceWayangPersistenceConfig config = AgenticCommerceWayangPersistenceConfig.fromMap(Map.of(
                "storageKind",
                "hosted-database"));
        AgenticCommerceWayangPersistenceStoreProviders providers =
                AgenticCommerceWayangPersistenceStoreProviders.builder()
                        .providers(AgenticCommerceWayangPersistenceStoreProviders.defaults())
                        .provider(new DatabaseProvider())
                        .build();

        AgenticCommerceWayangPersistenceStore store = config.buildStore(providers);

        assertThat(providers.storageKinds()).contains("hosted-database");
        assertThat(config.validationReport(providers).valid()).isTrue();
        assertThat(store.storageKind()).isEqualTo("hosted-database");
        assertThat(store.toMap()).containsEntry("storageKind", "hosted-database");
    }

    private static final class DatabaseProvider implements AgenticCommerceWayangPersistenceStoreProvider {

        @Override
        public String storageKind() {
            return "hosted-database";
        }

        @Override
        public boolean supports(AgenticCommerceWayangPersistenceConfig config) {
            return "hosted-database".equals(config.storageKind());
        }

        @Override
        public AgenticCommerceWayangPersistenceStore build(AgenticCommerceWayangPersistenceProviderContext context) {
            return new NamedPersistenceStore("hosted-database");
        }

        @Override
        public Map<String, Object> toMap() {
            return Map.of("storageKind", storageKind(), "custom", true);
        }
    }

    private static final class NamedPersistenceStore implements AgenticCommerceWayangPersistenceStore {

        private final String storageKind;
        private final InMemoryAgenticCommerceWayangPersistenceStore delegate =
                InMemoryAgenticCommerceWayangPersistenceStore.create();

        private NamedPersistenceStore(String storageKind) {
            this.storageKind = storageKind;
        }

        @Override
        public String storageKind() {
            return storageKind;
        }

        @Override
        public Optional<AgenticCommerceWayangRuntimeConfig> loadRuntimeConfig() {
            return delegate.loadRuntimeConfig();
        }

        @Override
        public void saveRuntimeConfig(AgenticCommerceWayangRuntimeConfig runtimeConfig) {
            delegate.saveRuntimeConfig(runtimeConfig);
        }

        @Override
        public Optional<AgenticCommerceWayangBootstrapConfig> loadBootstrapConfig() {
            return delegate.loadBootstrapConfig();
        }

        @Override
        public void saveBootstrapConfig(AgenticCommerceWayangBootstrapConfig bootstrapConfig) {
            delegate.saveBootstrapConfig(bootstrapConfig);
        }

        @Override
        public Optional<Map<String, Object>> loadBootstrapReport() {
            return delegate.loadBootstrapReport();
        }

        @Override
        public void saveBootstrapReport(AgenticCommerceWayangBootstrapReport bootstrapReport) {
            delegate.saveBootstrapReport(bootstrapReport);
        }

        @Override
        public Optional<Map<String, Object>> loadManifest() {
            return delegate.loadManifest();
        }

        @Override
        public void saveManifest(AgenticCommerceWayangManifest manifest) {
            delegate.saveManifest(manifest);
        }

        @Override
        public Map<String, Object> toMap() {
            return Map.of("storageKind", storageKind);
        }
    }
}

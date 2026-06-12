package tech.kayys.wayang.agenticcommerce.wayang;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class HybridAgenticCommerceWayangPersistenceStoreTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void primaryWinsWhenBothStoresHaveValues() {
        FileAgenticCommerceWayangPersistenceStore primary =
                FileAgenticCommerceWayangPersistenceStore.at(temporaryDirectory.resolve("primary"));
        FileAgenticCommerceWayangPersistenceStore fallback =
                FileAgenticCommerceWayangPersistenceStore.at(temporaryDirectory.resolve("fallback"));
        primary.saveRuntimeConfig(runtimeConfig("https://primary.example"));
        fallback.saveRuntimeConfig(runtimeConfig("https://fallback.example"));

        HybridAgenticCommerceWayangPersistenceStore hybrid =
                HybridAgenticCommerceWayangPersistenceStore.of(primary, fallback);

        assertThat(hybrid.loadRuntimeConfig().orElseThrow().connectorConfig().baseUrl())
                .isEqualTo("https://primary.example");
        assertThat(hybrid.toMap())
                .containsEntry("storageKind", HybridAgenticCommerceWayangPersistenceStore.STORAGE_KIND)
                .containsEntry("primaryStorageKind", FileAgenticCommerceWayangPersistenceStore.STORAGE_KIND)
                .containsEntry("fallbackStorageKind", FileAgenticCommerceWayangPersistenceStore.STORAGE_KIND)
                .containsEntry("mirrorWritesToFallback", true);
        assertThat(map(hybrid.toMap().get("target")))
                .containsEntry("targetKind", "hybrid")
                .containsEntry("primaryStorageKind", FileAgenticCommerceWayangPersistenceStore.STORAGE_KIND)
                .containsEntry("fallbackStorageKind", FileAgenticCommerceWayangPersistenceStore.STORAGE_KIND)
                .containsEntry("mirrorWritesToFallback", true);
    }

    @Test
    void fallbackIsUsedWhenPrimaryHasNoValueAndWritesAreMirrored() {
        FileAgenticCommerceWayangPersistenceStore primary =
                FileAgenticCommerceWayangPersistenceStore.at(temporaryDirectory.resolve("primary"));
        FileAgenticCommerceWayangPersistenceStore fallback =
                FileAgenticCommerceWayangPersistenceStore.at(temporaryDirectory.resolve("fallback"));
        fallback.saveBootstrapConfig(AgenticCommerceWayangBootstrapConfig.runtimeSkillsOnly());
        HybridAgenticCommerceWayangPersistenceStore hybrid =
                HybridAgenticCommerceWayangPersistenceStore.of(primary, fallback);

        AgenticCommerceWayangBootstrapConfig loaded = hybrid.loadBootstrapConfig().orElseThrow();
        hybrid.saveRuntimeConfig(runtimeConfig("https://seller.example"));

        assertThat(loaded.includeDefinitions()).isFalse();
        assertThat(loaded.includeRuntimeSkills()).isTrue();
        assertThat(primary.loadRuntimeConfig().orElseThrow().connectorConfig().baseUrl())
                .isEqualTo("https://seller.example");
        assertThat(fallback.loadRuntimeConfig().orElseThrow().connectorConfig().baseUrl())
                .isEqualTo("https://seller.example");
    }

    @Test
    void fallbackReceivesWritesWhenPrimaryFails() {
        FileAgenticCommerceWayangPersistenceStore fallback =
                FileAgenticCommerceWayangPersistenceStore.at(temporaryDirectory.resolve("fallback"));
        HybridAgenticCommerceWayangPersistenceStore hybrid =
                HybridAgenticCommerceWayangPersistenceStore.configured(
                        new FailingPersistenceStore(),
                        fallback,
                        false);

        hybrid.saveRuntimeConfig(runtimeConfig("https://fallback-write.example"));

        assertThat(hybrid.loadRuntimeConfig().orElseThrow().connectorConfig().baseUrl())
                .isEqualTo("https://fallback-write.example");
        assertThat(fallback.loadRuntimeConfig()).isPresent();
    }

    private static AgenticCommerceWayangRuntimeConfig runtimeConfig(String baseUrl) {
        return AgenticCommerceWayangRuntimeConfig.builder()
                .connectorConfig(AgenticCommerceConnectorConfig.bearer("seller-token")
                        .withBaseUrl(baseUrl))
                .build();
    }

    private static Map<String, Object> map(Object value) {
        assertThat(value).isInstanceOf(Map.class);
        return AgenticCommerceWayangMaps.copy((Map<?, ?>) value);
    }

    private static final class FailingPersistenceStore implements AgenticCommerceWayangPersistenceStore {

        @Override
        public String storageKind() {
            return "failing";
        }

        @Override
        public Optional<AgenticCommerceWayangRuntimeConfig> loadRuntimeConfig() {
            throw failure();
        }

        @Override
        public void saveRuntimeConfig(AgenticCommerceWayangRuntimeConfig runtimeConfig) {
            throw failure();
        }

        @Override
        public Optional<AgenticCommerceWayangBootstrapConfig> loadBootstrapConfig() {
            throw failure();
        }

        @Override
        public void saveBootstrapConfig(AgenticCommerceWayangBootstrapConfig bootstrapConfig) {
            throw failure();
        }

        @Override
        public Optional<Map<String, Object>> loadBootstrapReport() {
            throw failure();
        }

        @Override
        public void saveBootstrapReport(AgenticCommerceWayangBootstrapReport bootstrapReport) {
            throw failure();
        }

        @Override
        public Optional<Map<String, Object>> loadManifest() {
            throw failure();
        }

        @Override
        public void saveManifest(AgenticCommerceWayangManifest manifest) {
            throw failure();
        }

        @Override
        public Map<String, Object> toMap() {
            return Map.of("storageKind", storageKind());
        }

        private static IllegalStateException failure() {
            return new IllegalStateException("primary unavailable");
        }
    }
}

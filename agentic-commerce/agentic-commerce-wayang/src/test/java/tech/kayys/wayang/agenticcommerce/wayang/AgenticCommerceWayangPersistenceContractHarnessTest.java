package tech.kayys.wayang.agenticcommerce.wayang;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class AgenticCommerceWayangPersistenceContractHarnessTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void fileStorePassesRoundTripContract() {
        FileAgenticCommerceWayangPersistenceStore store =
                FileAgenticCommerceWayangPersistenceStore.at(temporaryDirectory.resolve("agentic-commerce"));

        AgenticCommerceWayangPersistenceContractReport report =
                AgenticCommerceWayangPersistenceContractHarness.roundTrip().run(store);

        assertThat(report.passed()).isTrue();
        assertThat(report.issueCount()).isZero();
        assertThat(report.persistedDocumentCount()).isEqualTo(4);
        assertThat(report.toMap())
                .containsEntry("passed", true)
                .containsEntry("persistedDocumentCount", 4);
        assertThat(report.persistenceTargetBefore())
                .containsEntry("targetKind", "file")
                .containsEntry("durable", true);
        assertThat(report.persistenceTargetAfter())
                .containsEntry("targetKind", "file")
                .containsEntry("durable", true);
        assertThat(map(report.toMap().get("persistenceTargetAfter")))
                .containsEntry("targetKind", "file")
                .containsEntry("durable", true);
        assertThat(report.toMap().toString())
                .doesNotContain(AgenticCommerceWayangPersistenceContractHarness.CONTRACT_BEARER_TOKEN);
        assertThat(store.loadRuntimeConfig().orElseThrow().connectorConfig().bearerToken())
                .isEqualTo(AgenticCommerceWayangPersistenceContractHarness.CONTRACT_BEARER_TOKEN);
        assertThat(store.loadBootstrapReport()).isPresent();
        assertThat(store.loadManifest()).isPresent();
    }

    @Test
    void inMemoryStorePassesRoundTripContract() {
        InMemoryAgenticCommerceWayangPersistenceStore store =
                InMemoryAgenticCommerceWayangPersistenceStore.create();

        AgenticCommerceWayangPersistenceContractReport report =
                AgenticCommerceWayangPersistenceContractHarness.roundTrip().run(store);

        assertThat(report.passed()).isTrue();
        assertThat(report.issueCount()).isZero();
        assertThat(report.persistedDocumentCount()).isEqualTo(4);
        assertThat(store.loadRuntimeConfig().orElseThrow().connectorConfig().bearerToken())
                .isEqualTo(AgenticCommerceWayangPersistenceContractHarness.CONTRACT_BEARER_TOKEN);
        assertThat(store.toMap())
                .containsEntry("storageKind", InMemoryAgenticCommerceWayangPersistenceStore.STORAGE_KIND)
                .containsEntry("documentCount", 4);
    }

    @Test
    void objectStorePassesRoundTripContract() {
        InMemoryAgenticCommerceObjectStoreClient client = InMemoryAgenticCommerceObjectStoreClient.create();
        ObjectStoreAgenticCommerceWayangPersistenceStore store =
                ObjectStoreAgenticCommerceWayangPersistenceStore.configured(
                        AgenticCommerceObjectStoreConfig.fromMap(Map.of(
                                "provider",
                                "s3",
                                "bucket",
                                "wayang-state",
                                "keyPrefix",
                                "contract")),
                        client);

        AgenticCommerceWayangPersistenceContractReport report =
                AgenticCommerceWayangPersistenceContractHarness.roundTrip().run(store);

        assertThat(report.passed()).isTrue();
        assertThat(client.contains("wayang-state", "contract/runtime-config.json")).isTrue();
        assertThat(client.contains("wayang-state", "contract/bootstrap-config.json")).isTrue();
        assertThat(client.contains("wayang-state", "contract/bootstrap-report.json")).isTrue();
        assertThat(client.contains("wayang-state", "contract/manifest.json")).isTrue();
        assertThat(report.persistenceTargetAfter())
                .containsEntry("targetKind", "object-store")
                .containsEntry("provider", AgenticCommerceObjectStoreConfig.PROVIDER_S3)
                .containsEntry("location", "wayang-state/contract")
                .containsEntry("cloudStorage", true);
        assertThat(number(map(report.toMap().get("manifest")).get("skillCount"))).isEqualTo(5);
    }

    @Test
    void persistenceServiceExposesContractHelper() {
        AgenticCommerceWayangPersistenceService service = AgenticCommerceWayangPersistenceService.of(
                FileAgenticCommerceWayangPersistenceStore.at(temporaryDirectory.resolve("service")));

        AgenticCommerceWayangPersistenceContractReport report = service.persistenceContract();

        assertThat(report.passed()).isTrue();
        assertThat(service.loadRuntimeConfig()).isPresent();
        assertThat(service.loadBootstrapConfig()).isPresent();
        assertThat(service.loadBootstrapReport()).isPresent();
        assertThat(service.loadManifest()).isPresent();
    }

    @Test
    void failingStoreReturnsIssueReportInsteadOfThrowing() {
        AgenticCommerceWayangPersistenceContractReport report =
                AgenticCommerceWayangPersistenceContractHarness.roundTrip().run(new FailingPersistenceStore());

        assertThat(report.passed()).isFalse();
        assertThat(report.issues())
                .contains(
                        "runtime_config_save_failed",
                        "runtime_config_load_failed",
                        "runtime_config_missing_after_save",
                        "manifest_missing_after_save");
        assertThat(report.toMap()).containsEntry("passed", false);
        assertThat(report.persistenceTargetBefore())
                .containsEntry("storageKind", "failing")
                .containsEntry("targetKind", "failing");
        assertThat(map(report.toMap().get("persistenceTargetAfter")))
                .containsEntry("storageKind", "failing")
                .containsEntry("targetKind", "failing");
    }

    private static Map<String, Object> map(Object value) {
        assertThat(value).isInstanceOf(Map.class);
        return AgenticCommerceWayangMaps.copy((Map<?, ?>) value);
    }

    private static int number(Object value) {
        assertThat(value).isInstanceOf(Number.class);
        return ((Number) value).intValue();
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
            throw failure();
        }

        private static IllegalStateException failure() {
            return new IllegalStateException("store unavailable");
        }
    }
}

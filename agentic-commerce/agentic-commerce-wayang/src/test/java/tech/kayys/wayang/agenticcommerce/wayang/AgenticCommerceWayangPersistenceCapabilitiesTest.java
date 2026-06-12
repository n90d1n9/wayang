package tech.kayys.wayang.agenticcommerce.wayang;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgenticCommerceWayangPersistenceCapabilitiesTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void detectsFileStoreAsDurableLocalPersistence() {
        AgenticCommerceWayangPersistenceCapabilities capabilities =
                AgenticCommerceWayangPersistenceCapabilities.from(
                        FileAgenticCommerceWayangPersistenceStore.at(temporaryDirectory.resolve("file")));

        assertThat(capabilities.storageKind()).isEqualTo(FileAgenticCommerceWayangPersistenceStore.STORAGE_KIND);
        assertThat(capabilities.durable()).isTrue();
        assertThat(capabilities.ephemeral()).isFalse();
        assertThat(capabilities.localFile()).isTrue();
        assertThat(capabilities.objectStore()).isFalse();
        assertThat(capabilities.storageKinds()).containsExactly(FileAgenticCommerceWayangPersistenceStore.STORAGE_KIND);
        assertThat(capabilities.toMap()).containsEntry("durable", true);
    }

    @Test
    void detectsInMemoryStoreAsEphemeralPersistence() {
        InMemoryAgenticCommerceWayangPersistenceStore store =
                InMemoryAgenticCommerceWayangPersistenceStore.create();

        AgenticCommerceWayangPersistenceCapabilities capabilities =
                AgenticCommerceWayangPersistenceCapabilities.from(store);

        assertThat(capabilities.storageKind()).isEqualTo(InMemoryAgenticCommerceWayangPersistenceStore.STORAGE_KIND);
        assertThat(capabilities.durable()).isFalse();
        assertThat(capabilities.ephemeral()).isTrue();
        assertThat(capabilities.localFile()).isFalse();
        assertThat(capabilities.objectStore()).isFalse();
        assertThat(capabilities.storageKinds())
                .containsExactly(InMemoryAgenticCommerceWayangPersistenceStore.STORAGE_KIND);
    }

    @Test
    void detectsObjectStoreAsDurableCloudPersistence() {
        ObjectStoreAgenticCommerceWayangPersistenceStore store =
                ObjectStoreAgenticCommerceWayangPersistenceStore.configured(
                        AgenticCommerceObjectStoreConfig.fromMap(Map.of(
                                "provider",
                                "rustfs",
                                "bucket",
                                "wayang-state",
                                "keyPrefix",
                                "prod")),
                        InMemoryAgenticCommerceObjectStoreClient.create());

        AgenticCommerceWayangPersistenceCapabilities capabilities =
                AgenticCommerceWayangPersistenceCapabilities.from(store);

        assertThat(capabilities.storageKind()).isEqualTo(ObjectStoreAgenticCommerceWayangPersistenceStore.STORAGE_KIND);
        assertThat(capabilities.durable()).isTrue();
        assertThat(capabilities.cloudStorage()).isTrue();
        assertThat(capabilities.objectStore()).isTrue();
        assertThat(capabilities.storageKinds())
                .containsExactly(ObjectStoreAgenticCommerceWayangPersistenceStore.STORAGE_KIND);
    }

    @Test
    void detectsHybridStoreLayersFromStatusMap() {
        AgenticCommerceWayangPersistenceCapabilities capabilities =
                AgenticCommerceWayangPersistenceCapabilities.fromStatus(Map.of(
                        "storageKind",
                        HybridAgenticCommerceWayangPersistenceStore.STORAGE_KIND,
                        "primaryStorageKind",
                        ObjectStoreAgenticCommerceWayangPersistenceStore.STORAGE_KIND,
                        "fallbackStorageKind",
                        FileAgenticCommerceWayangPersistenceStore.STORAGE_KIND,
                        "mirrorWritesToFallback",
                        true));

        assertThat(capabilities.hybrid()).isTrue();
        assertThat(capabilities.durable()).isTrue();
        assertThat(capabilities.cloudStorage()).isTrue();
        assertThat(capabilities.localFile()).isTrue();
        assertThat(capabilities.mirrored()).isTrue();
        assertThat(capabilities.fallbackReadable()).isTrue();
        assertThat(capabilities.storageKinds())
                .containsExactly(
                        HybridAgenticCommerceWayangPersistenceStore.STORAGE_KIND,
                        ObjectStoreAgenticCommerceWayangPersistenceStore.STORAGE_KIND,
                        FileAgenticCommerceWayangPersistenceStore.STORAGE_KIND);
        assertThat(map(capabilities.toMap().get("attributes")))
                .containsEntry("primaryStorageKind", ObjectStoreAgenticCommerceWayangPersistenceStore.STORAGE_KIND)
                .containsEntry("fallbackStorageKind", FileAgenticCommerceWayangPersistenceStore.STORAGE_KIND);
    }

    @Test
    void unknownDurableStoreKindIsTreatedAsDurableExtensionPoint() {
        AgenticCommerceWayangPersistenceCapabilities capabilities =
                AgenticCommerceWayangPersistenceCapabilities.fromStatus(Map.of(
                        "storageKind",
                        "database"));

        assertThat(capabilities.storageKind()).isEqualTo("database");
        assertThat(capabilities.durable()).isTrue();
        assertThat(capabilities.ephemeral()).isFalse();
        assertThat(capabilities.storageKinds()).containsExactly("database");
    }

    private static Map<String, Object> map(Object value) {
        assertThat(value).isInstanceOf(Map.class);
        return AgenticCommerceWayangMaps.copy((Map<?, ?>) value);
    }
}

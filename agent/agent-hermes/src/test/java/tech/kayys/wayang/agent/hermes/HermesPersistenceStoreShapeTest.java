package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HermesPersistenceStoreShapeTest {

    @Test
    void describesDurabilityAndBackendCapabilities() {
        assertThat(storageMetadata("noop"))
                .containsEntry("durable", false)
                .containsEntry("fileFallback", false)
                .containsEntry("objectStorageCapable", false)
                .containsEntry("databaseCapable", false);
        assertThat(storageMetadata("in-memory"))
                .containsEntry("durable", false)
                .containsEntry("fileFallback", false);
        assertThat(storageMetadata("file-system"))
                .containsEntry("durable", true)
                .containsEntry("fileFallback", true)
                .containsEntry("objectStorageCapable", false)
                .containsEntry("databaseCapable", false);
        assertThat(storageMetadata("object-storage"))
                .containsEntry("durable", true)
                .containsEntry("fileFallback", false)
                .containsEntry("objectStorageCapable", true)
                .containsEntry("databaseCapable", false);
        assertThat(storageMetadata("database"))
                .containsEntry("durable", true)
                .containsEntry("fileFallback", false)
                .containsEntry("objectStorageCapable", false)
                .containsEntry("databaseCapable", true);
        assertThat(storageMetadata("hybrid"))
                .containsEntry("durable", true)
                .containsEntry("fileFallback", true)
                .containsEntry("objectStorageCapable", true)
                .containsEntry("databaseCapable", true);
    }

    @Test
    void runtimeJournalDurabilityFollowsEnabledFlag() {
        Map<String, Object> disabled = new LinkedHashMap<>();
        HermesPersistenceStoreShape.of("database").putRuntimeJournalMetadata(disabled, false);

        assertThat(disabled)
                .containsEntry("durable", false)
                .containsEntry("databaseCapable", true)
                .containsEntry("queryable", false);

        Map<String, Object> enabled = new LinkedHashMap<>();
        HermesPersistenceStoreShape.of("database").putRuntimeJournalMetadata(enabled, true);

        assertThat(enabled)
                .containsEntry("durable", true)
                .containsEntry("databaseCapable", true)
                .containsEntry("queryable", true);
    }

    private static Map<String, Object> storageMetadata(String store) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        HermesPersistenceStoreShape.of(store).putStorageMetadata(metadata);
        return metadata;
    }
}

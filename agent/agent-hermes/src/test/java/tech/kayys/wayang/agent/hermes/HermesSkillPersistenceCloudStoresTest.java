package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HermesSkillPersistenceCloudStoresTest {

    @Test
    void parsesCanonicalObjectStoreNames() {
        assertThat(HermesSkillPersistenceCloudStores.parse("Amazon S3; RustFS, database, S3-compatible"))
                .containsExactly("s3", "rustfs", "s3-compatible");
    }

    @Test
    void derivesExplicitHintsBeforeConfiguredStores() {
        assertThat(HermesSkillPersistenceCloudStores.fromHints(
                Map.of("object-stores", "minio,s3"),
                "rustfs",
                "file-system",
                "Google Cloud Storage",
                "s3"))
                .containsExactly("minio", "s3", "rustfs", "gcs");
    }

    @Test
    void ignoresBlankAndNonCloudValues() {
        assertThat(HermesSkillPersistenceCloudStores.fromValues(
                " , ; database",
                null,
                "postgres",
                "Azure Blob Storage"))
                .containsExactly("azure-blob");
    }
}

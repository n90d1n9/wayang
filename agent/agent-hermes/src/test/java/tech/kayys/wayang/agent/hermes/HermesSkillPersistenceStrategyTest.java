package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HermesSkillPersistenceStrategyTest {

    @Test
    void defaultsUseSkillManagementStoresWithFileFallback() {
        HermesSkillPersistenceStrategy strategy = HermesSkillPersistenceStrategy.defaults();

        assertThat(strategy.definitionStore()).isEqualTo("skill-management.definition-store");
        assertThat(strategy.artifactStore()).isEqualTo("skill-management.artifact-store");
        assertThat(strategy.fallbackStore()).isEqualTo("file-system");
        assertThat(strategy.hasFileFallback()).isTrue();
    }

    @Test
    void detectsDatabaseCloudAndHybridPersistence() {
        HermesSkillPersistenceStrategy strategy = HermesSkillPersistenceStrategy.fromHints(Map.of(
                "definitions", "database",
                "artifacts", "s3",
                "fallback", "file-system"));

        assertThat(strategy.usesHybridPersistence()).isTrue();
        assertThat(strategy.usesDatabaseDefinitions()).isTrue();
        assertThat(strategy.usesCloudArtifacts()).isTrue();
        assertThat(strategy.cloudStores()).containsExactly("s3");
        assertThat(strategy.routePlan().routes()).extracting(HermesSkillPersistenceRoute::storeType)
                .containsExactly("database", "object-storage", "file");
    }

    @Test
    void readsExplicitCloudStoreHints() {
        HermesSkillPersistenceStrategy strategy = HermesSkillPersistenceStrategy.fromHints(Map.of(
                "definitions", "database",
                "artifacts", "rustfs",
                "cloudStores", "s3,minio"));

        assertThat(strategy.cloudStores()).containsExactly("s3", "minio", "rustfs");
        assertThat(strategy.toMetadata())
                .containsEntry("usesDatabaseDefinitions", true)
                .containsEntry("usesCloudArtifacts", true)
                .containsKey("routePlan");
    }

    @Test
    void acceptsAliasKeysForStoreHints() {
        HermesSkillPersistenceStrategy strategy = HermesSkillPersistenceStrategy.fromHints(Map.of(
                "definition-store", "postgres",
                "artifactStore", "gcs",
                "fallbackStore", "files",
                "object-stores", "s3;rustfs"));

        assertThat(strategy.definitionStore()).isEqualTo("postgres");
        assertThat(strategy.artifactStore()).isEqualTo("gcs");
        assertThat(strategy.fallbackStore()).isEqualTo("files");
        assertThat(strategy.cloudStores()).containsExactly("s3", "rustfs", "gcs");
    }
}

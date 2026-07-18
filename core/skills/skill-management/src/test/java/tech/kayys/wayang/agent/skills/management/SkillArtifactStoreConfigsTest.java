package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SkillArtifactStoreConfigsTest {

    @Test
    void parsesHybridObjectPrimaryWithFileFallbackFromProperties(@TempDir Path tempDir) {
        Properties properties = new Properties();
        properties.setProperty("wayang.skills.artifacts.store.kind", "hybrid");
        properties.setProperty("wayang.skills.artifacts.store.primary.kind", "rustfs");
        properties.setProperty("wayang.skills.artifacts.store.primary.object-prefix", "tenant-a/artifacts");
        properties.setProperty("wayang.skills.artifacts.store.fallback.kind", "filesystem");
        properties.setProperty("wayang.skills.artifacts.store.fallback.directory", tempDir.toString());

        SkillArtifactStoreConfig config = SkillArtifactStoreConfigs.fromProperties(properties);

        assertThat(config.kind()).isEqualTo(SkillArtifactStoreConfig.Kind.HYBRID);
        assertThat(config.primary().kind()).isEqualTo(SkillArtifactStoreConfig.Kind.OBJECT_STORAGE);
        assertThat(config.primary().objectPrefix()).isEqualTo("tenant-a/artifacts");
        assertThat(config.fallback().kind()).isEqualTo(SkillArtifactStoreConfig.Kind.FILESYSTEM);
        assertThat(config.fallback().directory()).isEqualTo(tempDir);
    }

    @Test
    void parsesObjectStorageArtifactStoreFromEnvironment() {
        SkillArtifactStoreConfig config = SkillArtifactStoreConfigs.fromEnvironment(Map.of(
                "WAYANG_SKILLS_ARTIFACTS_STORE_KIND", "s3",
                "WAYANG_SKILLS_ARTIFACTS_STORE_OBJECT_PREFIX", "tenant-a/artifacts"));

        assertThat(config.kind()).isEqualTo(SkillArtifactStoreConfig.Kind.OBJECT_STORAGE);
        assertThat(config.objectPrefix()).isEqualTo("tenant-a/artifacts");
    }

    @Test
    void parsesJdbcArtifactStoreFromProperties() {
        Properties properties = new Properties();
        properties.setProperty("wayang.skills.artifacts.store.kind", "database");
        properties.setProperty("wayang.skills.artifacts.store.jdbc.table-name", "skill_artifacts");
        properties.setProperty("wayang.skills.artifacts.store.jdbc.initialize-schema", "false");

        SkillArtifactStoreConfig config = SkillArtifactStoreConfigs.fromProperties(properties);

        assertThat(config.kind()).isEqualTo(SkillArtifactStoreConfig.Kind.JDBC);
        assertThat(config.jdbcTableName()).isEqualTo("skill_artifacts");
        assertThat(config.initializeJdbcSchema()).isFalse();
    }

    @Test
    void parsesNestedMapConfig() {
        SkillArtifactStoreConfig config = SkillArtifactStoreConfigs.fromMap(Map.of(
                "wayang", Map.of(
                        "skills", Map.of(
                                "artifacts", Map.of(
                                        "store", Map.of(
                                                "kind", "custom",
                                                "custom", Map.of("name", "tenant-artifacts")))))));

        assertThat(config.kind()).isEqualTo(SkillArtifactStoreConfig.Kind.CUSTOM);
        assertThat(config.customStoreName()).isEqualTo("tenant-artifacts");
    }

    @Test
    void defaultsToMemory() {
        assertThat(SkillArtifactStoreConfigs.fromMap(Map.of()).kind())
                .isEqualTo(SkillArtifactStoreConfig.Kind.MEMORY);
    }

    @Test
    void rejectsFilesystemWithoutDirectory() {
        assertThatThrownBy(() -> SkillArtifactStoreConfigs.fromMap(Map.of(
                "wayang.skills.artifacts.store.kind", "filesystem")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("directory");
    }

    @Test
    void rejectsCustomWithoutName() {
        assertThatThrownBy(() -> SkillArtifactStoreConfigs.fromMap(Map.of(
                "wayang.skills.artifacts.store.kind", "custom")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("store name");
    }

    @Test
    void rejectsHybridWithoutChildren() {
        assertThatThrownBy(() -> SkillArtifactStoreConfigs.fromMap(Map.of(
                "wayang.skills.artifacts.store.kind", "hybrid")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("primary and fallback");
    }

    @Test
    void validatesArtifactStoreConfigWithoutConstructing() {
        SkillStoreConfigValidationResult result = SkillArtifactStoreConfig.validate(
            SkillArtifactStoreConfig.Kind.FILESYSTEM,
            null,
            null,
            null,
            null,
            null,
            null);

        assertThat(result.validConfiguration()).isFalse();
        assertThat(result.errors()).containsExactly("Filesystem artifact store requires a directory");
    }

    @Test
    void rejectsJdbcWithoutTableName() {
        SkillStoreConfigValidationResult result = SkillArtifactStoreConfig.validate(
                SkillArtifactStoreConfig.Kind.JDBC,
                null,
                null,
                "",
                null,
                null,
                null);

        assertThat(result.validConfiguration()).isFalse();
        assertThat(result.errors()).containsExactly("JDBC artifact store requires a table name");
    }
}

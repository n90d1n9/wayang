package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SkillLifecycleStateStoreConfigsTest {

    @Test
    void parsesJdbcLifecycleStoreFromProperties() {
        Properties properties = new Properties();
        properties.setProperty("wayang.skills.lifecycle.store.kind", "database");
        properties.setProperty("wayang.skills.lifecycle.store.jdbc.table-name", "skill_lifecycle");
        properties.setProperty("wayang.skills.lifecycle.store.jdbc.initialize-schema", "false");

        SkillLifecycleStateStoreConfig config = SkillLifecycleStateStoreConfigs.fromProperties(properties);

        assertThat(config.kind()).isEqualTo(SkillLifecycleStateStoreConfig.Kind.JDBC);
        assertThat(config.jdbcTableName()).isEqualTo("skill_lifecycle");
        assertThat(config.initializeJdbcSchema()).isFalse();
    }

    @Test
    void parsesFilesystemLifecycleStoreFromEnvironment(@TempDir Path tempDir) {
        SkillLifecycleStateStoreConfig config = SkillLifecycleStateStoreConfigs.fromEnvironment(Map.of(
                "WAYANG_SKILLS_LIFECYCLE_STORE_KIND", "fs",
                "WAYANG_SKILLS_LIFECYCLE_STORE_DIRECTORY", tempDir.toString()));

        assertThat(config.kind()).isEqualTo(SkillLifecycleStateStoreConfig.Kind.FILESYSTEM);
        assertThat(config.directory()).isEqualTo(tempDir);
    }

    @Test
    void parsesObjectStorageLifecycleStoreFromEnvironment() {
        SkillLifecycleStateStoreConfig config = SkillLifecycleStateStoreConfigs.fromEnvironment(Map.of(
                "WAYANG_SKILLS_LIFECYCLE_STORE_KIND", "rustfs",
                "WAYANG_SKILLS_LIFECYCLE_STORE_OBJECT_PREFIX", "tenant-a/lifecycle"));

        assertThat(config.kind()).isEqualTo(SkillLifecycleStateStoreConfig.Kind.OBJECT_STORAGE);
        assertThat(config.objectPrefix()).isEqualTo("tenant-a/lifecycle");
    }

    @Test
    void parsesNestedMapConfig() {
        SkillLifecycleStateStoreConfig config = SkillLifecycleStateStoreConfigs.fromMap(Map.of(
                "wayang", Map.of(
                        "skills", Map.of(
                                "lifecycle", Map.of(
                                        "store", Map.of("kind", "memory"))))));

        assertThat(config.kind()).isEqualTo(SkillLifecycleStateStoreConfig.Kind.MEMORY);
    }

    @Test
    void parsesHybridLifecycleStoreFromNestedMap() {
        SkillLifecycleStateStoreConfig config = SkillLifecycleStateStoreConfigs.fromMap(Map.of(
                "wayang", Map.of(
                        "skills", Map.of(
                                "lifecycle", Map.of(
                                        "store", Map.of(
                                                "kind", "hybrid",
                                                "primary", Map.of("kind", "memory"),
                                                "fallback", Map.of(
                                                        "kind", "object",
                                                        "prefix", "tenant-a/lifecycle")))))));

        assertThat(config.kind()).isEqualTo(SkillLifecycleStateStoreConfig.Kind.HYBRID);
        assertThat(config.primary().kind()).isEqualTo(SkillLifecycleStateStoreConfig.Kind.MEMORY);
        assertThat(config.fallback().kind()).isEqualTo(SkillLifecycleStateStoreConfig.Kind.OBJECT_STORAGE);
        assertThat(config.fallback().objectPrefix()).isEqualTo("tenant-a/lifecycle");
    }

    @Test
    void parsesCustomLifecycleStoreFromProperties() {
        Properties properties = new Properties();
        properties.setProperty("wayang.skills.lifecycle.store.kind", "external");
        properties.setProperty("wayang.skills.lifecycle.store.custom.name", "redis-lifecycle");

        SkillLifecycleStateStoreConfig config = SkillLifecycleStateStoreConfigs.fromProperties(properties);

        assertThat(config.kind()).isEqualTo(SkillLifecycleStateStoreConfig.Kind.CUSTOM);
        assertThat(config.customStoreName()).isEqualTo("redis-lifecycle");
    }

    @Test
    void rejectsFilesystemWithoutDirectory() {
        assertThatThrownBy(() -> SkillLifecycleStateStoreConfigs.fromMap(Map.of(
                "wayang.skills.lifecycle.store.kind", "filesystem")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("directory");
    }

    @Test
    void rejectsCustomLifecycleStoreWithoutName() {
        assertThatThrownBy(() -> SkillLifecycleStateStoreConfigs.fromMap(Map.of(
                "wayang.skills.lifecycle.store.kind", "custom")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("store name");
    }

    @Test
    void rejectsHybridLifecycleStoreWithoutChildren() {
        assertThatThrownBy(() -> SkillLifecycleStateStoreConfigs.fromMap(Map.of(
                "wayang.skills.lifecycle.store.kind", "hybrid")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("primary and fallback");
    }

    @Test
    void validatesLifecycleStoreConfigWithoutConstructing() {
        SkillStoreConfigValidationResult result = SkillLifecycleStateStoreConfig.validate(
                SkillLifecycleStateStoreConfig.Kind.JDBC,
                null,
                null,
                " ",
                true,
                null,
                null,
                null);

        assertThat(result.validConfiguration()).isFalse();
        assertThat(result.errors()).containsExactly("JDBC lifecycle store requires a table name");
    }
}

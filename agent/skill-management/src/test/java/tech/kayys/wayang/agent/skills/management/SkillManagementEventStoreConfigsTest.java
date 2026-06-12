package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SkillManagementEventStoreConfigsTest {

    @Test
    void parsesFilesystemEventStoreFromProperties(@TempDir Path tempDir) {
        Properties properties = new Properties();
        properties.setProperty("wayang.skills.events.store.kind", "file");
        properties.setProperty("wayang.skills.events.store.directory", tempDir.resolve("events").toString());
        properties.setProperty("wayang.skills.events.store.max-events", "250");

        SkillManagementEventStoreConfig config = SkillManagementEventStoreConfigs.fromProperties(properties);

        assertThat(config.kind()).isEqualTo(SkillManagementEventStoreConfig.Kind.FILESYSTEM);
        assertThat(config.directory()).isEqualTo(tempDir.resolve("events"));
        assertThat(config.maxEvents()).isEqualTo(250);
    }

    @Test
    void parsesMemoryEventStoreFromEnvironment() {
        SkillManagementEventStoreConfig config = SkillManagementEventStoreConfigs.fromEnvironment(Map.of(
                "WAYANG_SKILLS_EVENTS_STORE_KIND", "memory",
                "WAYANG_SKILLS_EVENTS_STORE_MAX_EVENTS", "50"));

        assertThat(config.kind()).isEqualTo(SkillManagementEventStoreConfig.Kind.MEMORY);
        assertThat(config.maxEvents()).isEqualTo(50);
    }

    @Test
    void parsesJdbcEventStoreFromProperties() {
        Properties properties = new Properties();
        properties.setProperty("wayang.skills.events.store.kind", "database");
        properties.setProperty("wayang.skills.events.store.table", "skill_events");
        properties.setProperty("wayang.skills.events.store.initialize-schema", "false");
        properties.setProperty("wayang.skills.events.store.max-events", "75");

        SkillManagementEventStoreConfig config = SkillManagementEventStoreConfigs.fromProperties(properties);

        assertThat(config.kind()).isEqualTo(SkillManagementEventStoreConfig.Kind.JDBC);
        assertThat(config.jdbcTableName()).isEqualTo("skill_events");
        assertThat(config.initializeJdbcSchema()).isFalse();
        assertThat(config.maxEvents()).isEqualTo(75);
    }

    @Test
    void parsesObjectStorageEventStoreFromProperties() {
        Properties properties = new Properties();
        properties.setProperty("wayang.skills.events.store.kind", "rustfs");
        properties.setProperty("wayang.skills.events.store.object-prefix", "tenant-a/events");
        properties.setProperty("wayang.skills.events.store.max-events", "125");

        SkillManagementEventStoreConfig config = SkillManagementEventStoreConfigs.fromProperties(properties);

        assertThat(config.kind()).isEqualTo(SkillManagementEventStoreConfig.Kind.OBJECT_STORAGE);
        assertThat(config.objectPrefix()).isEqualTo("tenant-a/events");
        assertThat(config.maxEvents()).isEqualTo(125);
    }

    @Test
    void parsesCustomEventStoreFromNestedMap() {
        SkillManagementEventStoreConfig config = SkillManagementEventStoreConfigs.fromMap(Map.of(
                "wayang", Map.of(
                        "skills", Map.of(
                                "events", Map.of(
                                        "store", Map.of(
                                                "kind", "external",
                                                "name", "audit-events"))))));

        assertThat(config.kind()).isEqualTo(SkillManagementEventStoreConfig.Kind.CUSTOM);
        assertThat(config.customStoreName()).isEqualTo("audit-events");
    }

    @Test
    void parsesHybridEventStoreFromNestedMap(@TempDir Path tempDir) {
        SkillManagementEventStoreConfig config = SkillManagementEventStoreConfigs.fromMap(Map.of(
                "wayang", Map.of(
                        "skills", Map.of(
                                "events", Map.of(
                                        "store", Map.of(
                                                "kind", "hybrid",
                                                "primary", Map.of(
                                                        "kind", "jdbc",
                                                        "table", "skill_events",
                                                        "max-events", "500"),
                                                "fallback", Map.of(
                                                        "kind", "filesystem",
                                                        "directory", tempDir.resolve("events").toString(),
                                                        "max-events", "50")))))));

        assertThat(config.kind()).isEqualTo(SkillManagementEventStoreConfig.Kind.HYBRID);
        assertThat(config.primary().kind()).isEqualTo(SkillManagementEventStoreConfig.Kind.JDBC);
        assertThat(config.primary().jdbcTableName()).isEqualTo("skill_events");
        assertThat(config.primary().maxEvents()).isEqualTo(500);
        assertThat(config.fallback().kind()).isEqualTo(SkillManagementEventStoreConfig.Kind.FILESYSTEM);
        assertThat(config.fallback().directory()).isEqualTo(tempDir.resolve("events"));
        assertThat(config.fallback().maxEvents()).isEqualTo(50);
    }

    @Test
    void parsesMirrorEventStoreFromNestedMap(@TempDir Path tempDir) {
        SkillManagementEventStoreConfig config = SkillManagementEventStoreConfigs.fromMap(Map.of(
                "wayang", Map.of(
                        "skills", Map.of(
                                "events", Map.of(
                                        "store", Map.of(
                                                "kind", "mirror",
                                                "primary", Map.of(
                                                        "kind", "memory",
                                                        "max-events", "500"),
                                                "fallback", Map.of(
                                                        "kind", "filesystem",
                                                        "directory", tempDir.resolve("events").toString(),
                                                        "max-events", "50")))))));

        assertThat(config.kind()).isEqualTo(SkillManagementEventStoreConfig.Kind.MIRRORED);
        assertThat(config.primary().kind()).isEqualTo(SkillManagementEventStoreConfig.Kind.MEMORY);
        assertThat(config.primary().maxEvents()).isEqualTo(500);
        assertThat(config.fallback().kind()).isEqualTo(SkillManagementEventStoreConfig.Kind.FILESYSTEM);
        assertThat(config.fallback().directory()).isEqualTo(tempDir.resolve("events"));
        assertThat(config.fallback().maxEvents()).isEqualTo(50);
    }

    @Test
    void defaultsToNoopEventStore() {
        SkillManagementEventStoreConfig config = SkillManagementEventStoreConfigs.fromMap(Map.of());

        assertThat(config.kind()).isEqualTo(SkillManagementEventStoreConfig.Kind.NONE);
    }

    @Test
    void rejectsFilesystemEventStoreWithoutDirectory() {
        assertThatThrownBy(() -> SkillManagementEventStoreConfigs.fromMap(Map.of(
                "wayang.skills.events.store.kind", "filesystem")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("directory");
    }

    @Test
    void rejectsCustomEventStoreWithoutName() {
        assertThatThrownBy(() -> SkillManagementEventStoreConfigs.fromMap(Map.of(
                "wayang.skills.events.store.kind", "custom")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("store name");
    }

    @Test
    void rejectsObjectStorageEventStoreWithoutPrefix() {
        assertThatThrownBy(() -> new SkillManagementEventStoreConfig(
                SkillManagementEventStoreConfig.Kind.OBJECT_STORAGE,
                null,
                "",
                10,
                null,
                null,
                false,
                null,
                null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("object prefix");
    }

    @Test
    void rejectsJdbcEventStoreWithoutTableName() {
        assertThatThrownBy(() -> new SkillManagementEventStoreConfig(
                SkillManagementEventStoreConfig.Kind.JDBC,
                null,
                null,
                10,
                null,
                "",
                true,
                null,
                null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("table name");
    }

    @Test
    void rejectsHybridEventStoreWithoutChildren() {
        assertThatThrownBy(() -> SkillManagementEventStoreConfigs.fromMap(Map.of(
                "wayang.skills.events.store.kind", "hybrid")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("primary and fallback");
    }

    @Test
    void rejectsMirroredEventStoreWithoutChildren() {
        assertThatThrownBy(() -> SkillManagementEventStoreConfigs.fromMap(Map.of(
                "wayang.skills.events.store.kind", "mirrored")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("primary and fallback");
    }

    @Test
    void validatesEventStoreConfigWithoutConstructing() {
        SkillStoreConfigValidationResult result = SkillManagementEventStoreConfig.validate(
                SkillManagementEventStoreConfig.Kind.OBJECT_STORAGE,
                null,
                "",
                10,
                null,
                null,
                false,
                null,
                null);

        assertThat(result.validConfiguration()).isFalse();
        assertThat(result.errors()).containsExactly("Object-storage event store requires an object prefix");
    }
}

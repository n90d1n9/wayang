package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class SkillManagementServiceConfigsTest {

    @Test
    void parsesDefinitionAndLifecycleStoresFromProperties(@TempDir Path tempDir) {
        Properties properties = new Properties();
        properties.setProperty("wayang.skills.store.kind", "filesystem");
        properties.setProperty("wayang.skills.store.directory", tempDir.resolve("definitions").toString());
        properties.setProperty("wayang.skills.lifecycle.store.kind", "jdbc");
        properties.setProperty("wayang.skills.lifecycle.store.jdbc.table-name", "skill_lifecycle");
        properties.setProperty("wayang.skills.lifecycle.store.jdbc.initialize-schema", "false");
        properties.setProperty("wayang.skills.events.store.kind", "filesystem");
        properties.setProperty("wayang.skills.events.store.directory", tempDir.resolve("events").toString());
        properties.setProperty("wayang.skills.events.store.max-events", "25");
        properties.setProperty("wayang.skills.artifacts.store.kind", "filesystem");
        properties.setProperty("wayang.skills.artifacts.store.directory", tempDir.resolve("artifacts").toString());
        properties.setProperty("wayang.skills.lifecycle.reconcile.mode", "bootstrap");

        SkillManagementServiceConfig config = SkillManagementServiceConfigs.fromProperties(properties);

        assertThat(config.definitionStore().kind()).isEqualTo(SkillDefinitionStoreConfig.Kind.FILESYSTEM);
        assertThat(config.definitionStore().directory()).isEqualTo(tempDir.resolve("definitions"));
        assertThat(config.lifecycleStateStore().kind()).isEqualTo(SkillLifecycleStateStoreConfig.Kind.JDBC);
        assertThat(config.lifecycleStateStore().jdbcTableName()).isEqualTo("skill_lifecycle");
        assertThat(config.lifecycleStateStore().initializeJdbcSchema()).isFalse();
        assertThat(config.eventStore().kind()).isEqualTo(SkillManagementEventStoreConfig.Kind.FILESYSTEM);
        assertThat(config.eventStore().directory()).isEqualTo(tempDir.resolve("events"));
        assertThat(config.eventStore().maxEvents()).isEqualTo(25);
        assertThat(config.artifactStore().kind()).isEqualTo(SkillArtifactStoreConfig.Kind.FILESYSTEM);
        assertThat(config.artifactStore().directory()).isEqualTo(tempDir.resolve("artifacts"));
        assertThat(config.lifecycleStateReconcileOptions().createMissingStates()).isTrue();
        assertThat(config.lifecycleStateReconcileOptions().removeOrphanedStates()).isFalse();
    }

    @Test
    void parsesDefinitionAndLifecycleStoresFromEnvironment(@TempDir Path tempDir) {
        SkillManagementServiceConfig config = SkillManagementServiceConfigs.fromEnvironment(Map.of(
                "WAYANG_SKILLS_STORE_KIND", "s3",
                "WAYANG_SKILLS_STORE_OBJECT_PREFIX", "tenants/acme/skills",
                "WAYANG_SKILLS_LIFECYCLE_STORE_KIND", "filesystem",
                "WAYANG_SKILLS_LIFECYCLE_STORE_DIRECTORY", tempDir.resolve("lifecycle").toString(),
                "WAYANG_SKILLS_EVENTS_STORE_KIND", "memory",
                "WAYANG_SKILLS_EVENTS_STORE_MAX_EVENTS", "75",
                "WAYANG_SKILLS_ARTIFACTS_STORE_KIND", "rustfs",
                "WAYANG_SKILLS_ARTIFACTS_STORE_OBJECT_PREFIX", "tenants/acme/artifacts",
                "WAYANG_SKILLS_LIFECYCLE_RECONCILE_MODE", "sync"));

        assertThat(config.definitionStore().kind()).isEqualTo(SkillDefinitionStoreConfig.Kind.OBJECT_STORAGE);
        assertThat(config.definitionStore().objectPrefix()).isEqualTo("tenants/acme/skills");
        assertThat(config.lifecycleStateStore().kind()).isEqualTo(SkillLifecycleStateStoreConfig.Kind.FILESYSTEM);
        assertThat(config.lifecycleStateStore().directory()).isEqualTo(tempDir.resolve("lifecycle"));
        assertThat(config.eventStore().kind()).isEqualTo(SkillManagementEventStoreConfig.Kind.MEMORY);
        assertThat(config.eventStore().maxEvents()).isEqualTo(75);
        assertThat(config.artifactStore().kind()).isEqualTo(SkillArtifactStoreConfig.Kind.OBJECT_STORAGE);
        assertThat(config.artifactStore().objectPrefix()).isEqualTo("tenants/acme/artifacts");
        assertThat(config.lifecycleStateReconcileOptions().createMissingStates()).isTrue();
        assertThat(config.lifecycleStateReconcileOptions().removeOrphanedStates()).isTrue();
    }

    @Test
    void parsesDefinitionAndLifecycleStoresFromNestedMap(@TempDir Path tempDir) {
        SkillManagementServiceConfig config = SkillManagementServiceConfigs.fromMap(Map.of(
                "wayang", Map.of(
                        "skills", Map.of(
                                "store", Map.of(
                                        "kind", "hybrid",
                                        "primary", Map.of(
                                                "kind", "jdbc",
                                                "table", "skill_defs"),
                                        "fallback", Map.of(
                                                "kind", "filesystem",
                                                "directory", tempDir.resolve("fallback").toString())),
                                "lifecycle", Map.of(
                                        "store", Map.of(
                                                "kind", "filesystem",
                                                "directory", tempDir.resolve("lifecycle").toString()),
                                        "reconcile", Map.of(
                                                "mode", "sync",
                                                "remove-orphans", "false")),
                                "events", Map.of(
                                        "store", Map.of(
                                                "kind", "custom",
                                                "name", "audit-events")),
                                "artifacts", Map.of(
                                        "store", Map.of(
                                                "kind", "hybrid",
                                                "primary", Map.of(
                                                        "kind", "object",
                                                        "prefix", "tenant-a/artifacts"),
                                                "fallback", Map.of(
                                                        "kind", "filesystem",
                                                        "directory", tempDir.resolve("artifacts").toString())))))));

        assertThat(config.definitionStore().kind()).isEqualTo(SkillDefinitionStoreConfig.Kind.HYBRID);
        assertThat(config.definitionStore().primary().kind()).isEqualTo(SkillDefinitionStoreConfig.Kind.JDBC);
        assertThat(config.definitionStore().fallback().directory()).isEqualTo(tempDir.resolve("fallback"));
        assertThat(config.lifecycleStateStore().kind()).isEqualTo(SkillLifecycleStateStoreConfig.Kind.FILESYSTEM);
        assertThat(config.lifecycleStateStore().directory()).isEqualTo(tempDir.resolve("lifecycle"));
        assertThat(config.eventStore().kind()).isEqualTo(SkillManagementEventStoreConfig.Kind.CUSTOM);
        assertThat(config.eventStore().customStoreName()).isEqualTo("audit-events");
        assertThat(config.artifactStore().kind()).isEqualTo(SkillArtifactStoreConfig.Kind.HYBRID);
        assertThat(config.artifactStore().primary().kind()).isEqualTo(SkillArtifactStoreConfig.Kind.OBJECT_STORAGE);
        assertThat(config.artifactStore().primary().objectPrefix()).isEqualTo("tenant-a/artifacts");
        assertThat(config.artifactStore().fallback().directory()).isEqualTo(tempDir.resolve("artifacts"));
        assertThat(config.lifecycleStateReconcileOptions().createMissingStates()).isTrue();
        assertThat(config.lifecycleStateReconcileOptions().removeOrphanedStates()).isFalse();
    }

    @Test
    void parsesMirroredStoresFromNestedMap(@TempDir Path tempDir) {
        SkillManagementServiceConfig config = SkillManagementServiceConfigs.fromMap(Map.of(
                "wayang", Map.of(
                        "skills", Map.of(
                                "store", Map.of(
                                        "kind", "mirror",
                                        "primary", Map.of(
                                                "kind", "s3",
                                                "prefix", "tenant-a/skills"),
                                        "fallback", Map.of(
                                                "kind", "filesystem",
                                                "directory", tempDir.resolve("definitions").toString())),
                                "lifecycle", Map.of(
                                        "store", Map.of(
                                                "kind", "replicated",
                                                "primary", Map.of(
                                                        "kind", "rustfs",
                                                        "prefix", "tenant-a/lifecycle"),
                                                "fallback", Map.of(
                                                        "kind", "filesystem",
                                                        "directory", tempDir.resolve("lifecycle").toString()))),
                                "artifacts", Map.of(
                                        "store", Map.of(
                                                "kind", "mirrored",
                                                "primary", Map.of(
                                                        "kind", "object",
                                                        "prefix", "tenant-a/artifacts"),
                                                "fallback", Map.of(
                                                        "kind", "filesystem",
                                                        "directory", tempDir.resolve("artifacts").toString()))),
                                "events", Map.of(
                                        "store", Map.of(
                                                "kind", "replicated",
                                                "primary", Map.of(
                                                        "kind", "memory",
                                                        "max-events", "500"),
                                                "fallback", Map.of(
                                                        "kind", "filesystem",
                                                        "directory", tempDir.resolve("events").toString(),
                                                        "max-events", "50")))))));

        assertThat(config.definitionStore().kind()).isEqualTo(SkillDefinitionStoreConfig.Kind.MIRRORED);
        assertThat(config.definitionStore().primary().kind()).isEqualTo(SkillDefinitionStoreConfig.Kind.OBJECT_STORAGE);
        assertThat(config.definitionStore().fallback().directory()).isEqualTo(tempDir.resolve("definitions"));
        assertThat(config.lifecycleStateStore().kind()).isEqualTo(SkillLifecycleStateStoreConfig.Kind.MIRRORED);
        assertThat(config.lifecycleStateStore().primary().kind())
                .isEqualTo(SkillLifecycleStateStoreConfig.Kind.OBJECT_STORAGE);
        assertThat(config.lifecycleStateStore().fallback().directory()).isEqualTo(tempDir.resolve("lifecycle"));
        assertThat(config.artifactStore().kind()).isEqualTo(SkillArtifactStoreConfig.Kind.MIRRORED);
        assertThat(config.artifactStore().primary().kind()).isEqualTo(SkillArtifactStoreConfig.Kind.OBJECT_STORAGE);
        assertThat(config.artifactStore().fallback().directory()).isEqualTo(tempDir.resolve("artifacts"));
        assertThat(config.eventStore().kind()).isEqualTo(SkillManagementEventStoreConfig.Kind.MIRRORED);
        assertThat(config.eventStore().primary().kind()).isEqualTo(SkillManagementEventStoreConfig.Kind.MEMORY);
        assertThat(config.eventStore().primary().maxEvents()).isEqualTo(500);
        assertThat(config.eventStore().fallback().kind()).isEqualTo(SkillManagementEventStoreConfig.Kind.FILESYSTEM);
        assertThat(config.eventStore().fallback().directory()).isEqualTo(tempDir.resolve("events"));
        assertThat(config.eventStore().fallback().maxEvents()).isEqualTo(50);
    }

    @Test
    void defaultsToRegistryDefinitionsAndMemoryLifecycleState() {
        SkillManagementServiceConfig config = SkillManagementServiceConfigs.fromMap(Map.of());

        assertThat(config.definitionStore().kind()).isEqualTo(SkillDefinitionStoreConfig.Kind.REGISTRY);
        assertThat(config.lifecycleStateStore().kind()).isEqualTo(SkillLifecycleStateStoreConfig.Kind.MEMORY);
        assertThat(config.eventStore().kind()).isEqualTo(SkillManagementEventStoreConfig.Kind.NONE);
        assertThat(config.artifactStore().kind()).isEqualTo(SkillArtifactStoreConfig.Kind.MEMORY);
        assertThat(config.lifecycleStateReconcileOptions()).isEqualTo(SkillLifecycleStateReconcileOptions.inspectOnly());
        assertThat(config.validate().validConfiguration()).isTrue();
    }

    @Test
    void parsesProfiledServiceConfigFromProperties(@TempDir Path tempDir) {
        Properties properties = new Properties();
        properties.setProperty("wayang.skills.profile", "local-filesystem");
        properties.setProperty("wayang.skills.profile.base-directory", tempDir.toString());
        properties.setProperty("wayang.skills.profile.max-events", "33");

        SkillManagementServiceConfig config = SkillManagementServiceConfigs.fromProperties(properties);

        assertThat(config.definitionStore().kind()).isEqualTo(SkillDefinitionStoreConfig.Kind.FILESYSTEM);
        assertThat(config.definitionStore().directory()).isEqualTo(tempDir.resolve("definitions"));
        assertThat(config.lifecycleStateStore().kind()).isEqualTo(SkillLifecycleStateStoreConfig.Kind.FILESYSTEM);
        assertThat(config.lifecycleStateStore().directory()).isEqualTo(tempDir.resolve("lifecycle"));
        assertThat(config.eventStore().kind()).isEqualTo(SkillManagementEventStoreConfig.Kind.FILESYSTEM);
        assertThat(config.eventStore().directory()).isEqualTo(tempDir.resolve("events"));
        assertThat(config.eventStore().maxEvents()).isEqualTo(33);
        assertThat(config.artifactStore().kind()).isEqualTo(SkillArtifactStoreConfig.Kind.FILESYSTEM);
        assertThat(config.artifactStore().directory()).isEqualTo(tempDir.resolve("artifacts"));
    }

    @Test
    void parsesProfiledServiceConfigFromEnvironment() {
        SkillManagementServiceConfig config = SkillManagementServiceConfigs.fromEnvironment(Map.of(
                "WAYANG_SKILLS_PROFILE", "rustfs",
                "WAYANG_SKILLS_PROFILE_OBJECT_PREFIX", "tenant-a/skills",
                "WAYANG_SKILLS_PROFILE_MAX_EVENTS", "44"));

        assertThat(config.definitionStore().kind()).isEqualTo(SkillDefinitionStoreConfig.Kind.OBJECT_STORAGE);
        assertThat(config.definitionStore().objectPrefix()).isEqualTo("tenant-a/skills/definitions");
        assertThat(config.lifecycleStateStore().objectPrefix()).isEqualTo("tenant-a/skills/lifecycle");
        assertThat(config.eventStore().kind()).isEqualTo(SkillManagementEventStoreConfig.Kind.OBJECT_STORAGE);
        assertThat(config.eventStore().objectPrefix()).isEqualTo("tenant-a/skills/events");
        assertThat(config.eventStore().maxEvents()).isEqualTo(44);
        assertThat(config.artifactStore().objectPrefix()).isEqualTo("tenant-a/skills/artifacts");
    }

    @Test
    void profileAllowsExplicitStoreKindOverride(@TempDir Path tempDir) {
        SkillManagementServiceConfig config = SkillManagementServiceConfigs.fromMap(Map.of(
                "wayang.skills.profile", "object-storage",
                "wayang.skills.profile.object-prefix", "tenant-a/skills",
                "wayang.skills.profile.max-events", "55",
                "wayang.skills.artifacts.store.kind", "filesystem",
                "wayang.skills.artifacts.store.directory", tempDir.resolve("artifacts").toString()));

        assertThat(config.definitionStore().kind()).isEqualTo(SkillDefinitionStoreConfig.Kind.OBJECT_STORAGE);
        assertThat(config.definitionStore().objectPrefix()).isEqualTo("tenant-a/skills/definitions");
        assertThat(config.eventStore().maxEvents()).isEqualTo(55);
        assertThat(config.artifactStore().kind()).isEqualTo(SkillArtifactStoreConfig.Kind.FILESYSTEM);
        assertThat(config.artifactStore().directory()).isEqualTo(tempDir.resolve("artifacts"));
    }

    @Test
    void validatesServiceConfigWithoutThrowing() {
        SkillStoreConfigValidationResult result = SkillManagementServiceConfigs.validateMap(Map.of(
                "wayang.skills.events.store.kind", "filesystem"));

        assertThat(result.validConfiguration()).isFalse();
        assertThat(result.errors()).containsExactly("Filesystem event store requires a directory");
    }

    @Test
    void validatesEnvironmentConfigWithoutThrowing() {
        SkillStoreConfigValidationResult result = SkillManagementServiceConfigs.validateEnvironment(Map.of(
                "WAYANG_SKILLS_STORE_KIND", "filesystem"));

        assertThat(result.validConfiguration()).isFalse();
        assertThat(result.errors()).containsExactly("Filesystem skill store requires a directory");
    }

    @Test
    void validatesArtifactStoreConfigWithoutThrowing() {
        SkillStoreConfigValidationResult result = SkillManagementServiceConfigs.validateMap(Map.of(
                "wayang.skills.artifacts.store.kind", "filesystem"));

        assertThat(result.validConfiguration()).isFalse();
        assertThat(result.errors()).containsExactly("Filesystem artifact store requires a directory");
    }
}

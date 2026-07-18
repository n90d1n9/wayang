package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class SkillPersistenceContractMatrixTest {

    @Test
    void derivesDefaultServicePersistenceContracts() {
        SkillPersistenceContractMatrix matrix = SkillPersistenceContractMatrix.from(null);

        assertThat(matrix.rows())
                .extracting(SkillPersistenceContractMatrix.Row::path)
                .containsExactly("definition", "lifecycle-state", "event-history", "artifact");

        SkillPersistenceContractMatrix.Row definition =
                matrix.row(SkillPersistenceContractMatrix.Role.DEFINITION).orElseThrow();
        assertThat(definition.provider()).isEqualTo("registry");
        assertThat(definition.persistenceClass())
                .isEqualTo(SkillPersistenceContractMatrix.PersistenceClass.RUNTIME_REGISTRY);
        assertThat(definition.capabilities()).containsExactly("read", "write", "delete", "list");
        assertThat(definition.durable()).isFalse();

        SkillPersistenceContractMatrix.Row lifecycle =
                matrix.row(SkillPersistenceContractMatrix.Role.LIFECYCLE_STATE).orElseThrow();
        assertThat(lifecycle.provider()).isEqualTo("memory");
        assertThat(lifecycle.ephemeral()).isTrue();

        SkillPersistenceContractMatrix.Row event =
                matrix.row(SkillPersistenceContractMatrix.Role.EVENT_HISTORY).orElseThrow();
        assertThat(event.provider()).isEqualTo("none");
        assertThat(event.disabled()).isTrue();
        assertThat(event.capabilities()).isEmpty();

        SkillPersistenceContractMatrix.Row artifact =
                matrix.row(SkillPersistenceContractMatrix.Role.ARTIFACT).orElseThrow();
        assertThat(artifact.provider()).isEqualTo("memory");
        assertThat(artifact.ephemeral()).isTrue();
    }

    @Test
    void derivesHybridMirroredDurabilityAndCapabilityContracts() {
        SkillManagementServiceConfig config = SkillManagementServiceConfig.of(
                SkillDefinitionStoreConfig.mirrored(
                        SkillDefinitionStoreConfig.objectStorage("skills/definitions"),
                        SkillDefinitionStoreConfig.fileSystem(Path.of("/tmp/skills/definitions"))),
                SkillLifecycleStateStoreConfig.hybrid(
                        SkillLifecycleStateStoreConfig.jdbc(),
                        SkillLifecycleStateStoreConfig.memory()),
                SkillManagementEventStoreConfig.hybrid(
                        SkillManagementEventStoreConfig.memory(),
                        SkillManagementEventStoreConfig.custom("audit-events")),
                SkillArtifactStoreConfig.jdbc(),
                SkillLifecycleStateReconcileOptions.inspectOnly());

        SkillPersistenceContractMatrix matrix = SkillPersistenceContractMatrix.from(config);

        SkillPersistenceContractMatrix.Row definition =
                matrix.row(SkillPersistenceContractMatrix.Role.DEFINITION).orElseThrow();
        assertThat(definition.provider()).isEqualTo("mirrored");
        assertThat(definition.mirrored()).isTrue();
        assertThat(definition.durable()).isTrue();
        assertThat(definition.durableFallback()).isTrue();
        assertThat(definition.capabilities())
                .contains("read", "write", "delete", "list", "primary-fallback", "mirror-write");
        assertThat(definition.children())
                .extracting(SkillPersistenceContractMatrix.Row::provider)
                .containsExactly("object-storage", "filesystem");

        SkillPersistenceContractMatrix.Row lifecycle =
                matrix.row(SkillPersistenceContractMatrix.Role.LIFECYCLE_STATE).orElseThrow();
        assertThat(lifecycle.provider()).isEqualTo("hybrid");
        assertThat(lifecycle.durable()).isTrue();
        assertThat(lifecycle.durableFallback()).isFalse();
        assertThat(lifecycle.capabilities()).contains("primary-fallback");

        SkillPersistenceContractMatrix.Row event =
                matrix.row(SkillPersistenceContractMatrix.Role.EVENT_HISTORY).orElseThrow();
        assertThat(event.provider()).isEqualTo("hybrid");
        assertThat(event.composite()).isTrue();
        assertThat(event.mirrored()).isFalse();
        assertThat(event.capabilities()).contains("write", "query-events", "composite");
        assertThat(event.capabilities()).doesNotContain("prune-events");
        assertThat(event.children())
                .extracting(SkillPersistenceContractMatrix.Row::provider)
                .containsExactly("memory", "custom");

        SkillPersistenceContractMatrix.Row artifact =
                matrix.row(SkillPersistenceContractMatrix.Role.ARTIFACT).orElseThrow();
        assertThat(artifact.provider()).isEqualTo("jdbc");
        assertThat(artifact.transactional()).isTrue();
        assertThat(artifact.durable()).isTrue();
        assertThat(artifact.external()).isTrue();
    }

    @Test
    void exposesFlattenedDurableCustomAndEphemeralSlices() {
        SkillManagementServiceConfig config = SkillManagementServiceConfig.of(
                SkillDefinitionStoreConfig.fileSystem(Path.of("/tmp/skills/definitions")),
                SkillLifecycleStateStoreConfig.memory(),
                SkillManagementEventStoreConfig.custom("audit-events"),
                SkillArtifactStoreConfig.objectStorage("skills/artifacts"),
                SkillLifecycleStateReconcileOptions.inspectOnly());

        SkillPersistenceContractMatrix matrix = SkillPersistenceContractMatrix.from(config);

        assertThat(matrix.flattenedRows())
                .extracting(SkillPersistenceContractMatrix.Row::path)
                .containsExactly("definition", "lifecycle-state", "event-history", "artifact");
        assertThat(matrix.durableRows())
                .extracting(SkillPersistenceContractMatrix.Row::path)
                .containsExactly("definition", "artifact");
        assertThat(matrix.customRows())
                .extracting(SkillPersistenceContractMatrix.Row::path)
                .containsExactly("event-history");
        assertThat(matrix.ephemeralRows())
                .extracting(SkillPersistenceContractMatrix.Row::path)
                .containsExactly("lifecycle-state", "event-history");
    }
}

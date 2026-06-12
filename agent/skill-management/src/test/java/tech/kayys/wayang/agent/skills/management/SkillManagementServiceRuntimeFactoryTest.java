package tech.kayys.wayang.agent.skills.management;

import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tech.kayys.wayang.agent.spi.skills.SkillDefinition;
import tech.kayys.wayang.storage.spi.ObjectStorageService;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SkillManagementServiceRuntimeFactoryTest {

    @Test
    void createsConfiguredFileBackedServiceForRuntimeFallback(@TempDir Path tempDir) {
        Path definitions = tempDir.resolve("definitions");
        Path lifecycle = tempDir.resolve("lifecycle");
        Path events = tempDir.resolve("events");
        Path artifacts = tempDir.resolve("artifacts");
        SkillManagementServiceConfig config = SkillManagementServiceConfig.of(
                SkillDefinitionStoreConfig.fileSystem(definitions),
                SkillLifecycleStateStoreConfig.fileSystem(lifecycle),
                SkillManagementEventStoreConfig.fileSystem(events, 50),
                SkillArtifactStoreConfig.fileSystem(artifacts),
                SkillLifecycleStateReconcileOptions.inspectOnly());

        SkillManagementService firstService =
                SkillManagementServiceRuntimeFactory.create(new TestSkillRegistry(), config);
        SkillDefinition skill = TestSkillDefinitions.basic("runtime-planner");
        SkillArtifactReference artifactReference =
                SkillArtifactReference.resource("runtime-planner", "prompt", "v1");

        firstService.createSkill(skill).await().indefinitely();
        firstService.disableSkill("runtime-planner").await().indefinitely();
        firstService.putArtifact(SkillArtifact.text(artifactReference, "Plan with persistence."))
                .await()
                .indefinitely();

        SkillManagementService secondService =
                SkillManagementServiceRuntimeFactory.create(new TestSkillRegistry(), config);

        assertThat(secondService.getSkill("runtime-planner").await().indefinitely())
                .isPresent()
                .get()
                .extracting(SkillDefinition::name, SkillDefinition::systemPrompt)
                .containsExactly("runtime-planner", "Do the thing.");
        assertThat(secondService.getLifecycleState("runtime-planner").await().indefinitely().status())
                .isEqualTo(SkillLifecycleStatus.DISABLED);
        assertThat(secondService.listArtifacts("runtime-planner").await().indefinitely())
                .containsExactly(artifactReference);
        assertThat(secondService.getArtifact(artifactReference).await().indefinitely())
                .isPresent()
                .get()
                .extracting(artifact -> new String(artifact.content(), StandardCharsets.UTF_8))
                .isEqualTo("Plan with persistence.");
        assertThat(Files.isDirectory(definitions)).isTrue();
        assertThat(Files.isDirectory(lifecycle)).isTrue();
        assertThat(Files.isDirectory(events)).isTrue();
        assertThat(Files.isDirectory(artifacts)).isTrue();
    }

    @Test
    void createsConfiguredObjectStorageBackedServiceForRuntimeFallback() {
        InMemoryPlatformObjectStorageService storage = new InMemoryPlatformObjectStorageService();
        SkillManagementServiceConfig config = SkillManagementServiceConfig.of(
                SkillDefinitionStoreConfig.objectStorage("tenant-a/definitions"),
                SkillLifecycleStateStoreConfig.objectStorage("tenant-a/lifecycle"),
                SkillManagementEventStoreConfig.objectStorage("tenant-a/events", 50),
                SkillArtifactStoreConfig.objectStorage("tenant-a/artifacts"),
                SkillLifecycleStateReconcileOptions.inspectOnly());

        SkillManagementService firstService = SkillManagementServiceRuntimeFactory.create(
                new TestSkillRegistry(),
                config,
                Optional.of(storage),
                Optional.empty());
        SkillDefinition skill = TestSkillDefinitions.basic("cloud-planner");
        SkillArtifactReference artifactReference =
                SkillArtifactReference.packageArtifact("cloud-planner", "v1");

        firstService.createSkill(skill).await().indefinitely();
        firstService.disableSkill("cloud-planner").await().indefinitely();
        firstService.putArtifact(SkillArtifact.text(artifactReference, "Cloud-backed skill package."))
                .await()
                .indefinitely();

        SkillManagementService secondService = SkillManagementServiceRuntimeFactory.create(
                new TestSkillRegistry(),
                config,
                Optional.of(storage),
                Optional.empty());

        assertThat(secondService.getSkill("cloud-planner").await().indefinitely())
                .isPresent()
                .get()
                .extracting(SkillDefinition::name)
                .isEqualTo("cloud-planner");
        assertThat(secondService.getLifecycleState("cloud-planner").await().indefinitely().status())
                .isEqualTo(SkillLifecycleStatus.DISABLED);
        assertThat(secondService.getArtifact(artifactReference).await().indefinitely())
                .isPresent()
                .get()
                .extracting(artifact -> new String(artifact.content(), StandardCharsets.UTF_8))
                .isEqualTo("Cloud-backed skill package.");
        assertThat(secondService.eventHistory(SkillManagementEventQuery.forSkill("cloud-planner", 10))
                .await()
                .indefinitely()
                .events())
                .extracting(SkillManagementEvent::operation)
                .contains(
                        SkillManagementEventOperation.CREATE_SKILL,
                        SkillManagementEventOperation.TRANSITION_SKILL,
                        SkillManagementEventOperation.PUT_ARTIFACT);
        assertThat(storage.keys()).anyMatch(key -> key.startsWith("tenant-a/definitions/"));
        assertThat(storage.keys()).anyMatch(key -> key.startsWith("tenant-a/lifecycle/"));
        assertThat(storage.keys()).anyMatch(key -> key.startsWith("tenant-a/events/"));
        assertThat(storage.keys()).anyMatch(key -> key.startsWith("tenant-a/artifacts/"));
    }

    @Test
    void createsConfiguredHybridCloudPrimaryWithFileFallback(@TempDir Path tempDir) {
        InMemoryPlatformObjectStorageService storage = new InMemoryPlatformObjectStorageService();
        Path fallbackDefinitions = tempDir.resolve("fallback-definitions");
        Path fallbackLifecycle = tempDir.resolve("fallback-lifecycle");
        Path fallbackArtifacts = tempDir.resolve("fallback-artifacts");
        SkillDefinition fallbackSkill = TestSkillDefinitions.basic("fallback-planner");
        SkillArtifactReference fallbackArtifact =
                SkillArtifactReference.resource("fallback-planner", "prompt", "v1");
        SkillArtifactReference primaryArtifact =
                SkillArtifactReference.packageArtifact("hybrid-primary", "v1");
        new FileSystemSkillDefinitionStore(fallbackDefinitions).registerSkill(fallbackSkill);
        new FileSystemSkillLifecycleStateStore(fallbackLifecycle)
                .save(SkillLifecycleState.created("fallback-planner"));
        new FileSystemSkillArtifactStore(fallbackArtifacts)
                .putArtifact(SkillArtifact.text(fallbackArtifact, "Fallback file prompt."));
        SkillManagementServiceConfig config = SkillManagementServiceConfig.of(
                SkillDefinitionStoreConfig.hybrid(
                        SkillDefinitionStoreConfig.objectStorage("tenant-a/definitions"),
                        SkillDefinitionStoreConfig.fileSystem(fallbackDefinitions)),
                SkillLifecycleStateStoreConfig.hybrid(
                        SkillLifecycleStateStoreConfig.objectStorage("tenant-a/lifecycle"),
                        SkillLifecycleStateStoreConfig.fileSystem(fallbackLifecycle)),
                SkillManagementEventStoreConfig.memory(50),
                SkillArtifactStoreConfig.hybrid(
                        SkillArtifactStoreConfig.objectStorage("tenant-a/artifacts"),
                        SkillArtifactStoreConfig.fileSystem(fallbackArtifacts)),
                SkillLifecycleStateReconcileOptions.inspectOnly());

        SkillManagementService service = SkillManagementServiceRuntimeFactory.create(
                new TestSkillRegistry(),
                config,
                Optional.of(storage),
                Optional.empty());

        assertThat(service.getSkill("fallback-planner").await().indefinitely())
                .contains(fallbackSkill);
        assertThat(service.getLifecycleState("fallback-planner").await().indefinitely().status())
                .isEqualTo(SkillLifecycleStatus.ACTIVE);
        assertThat(service.getArtifact(fallbackArtifact).await().indefinitely())
                .isPresent()
                .get()
                .extracting(artifact -> new String(artifact.content(), StandardCharsets.UTF_8))
                .isEqualTo("Fallback file prompt.");

        service.createSkill(TestSkillDefinitions.basic("hybrid-primary")).await().indefinitely();
        service.putArtifact(SkillArtifact.text(primaryArtifact, "Primary cloud package."))
                .await()
                .indefinitely();

        assertThat(storage.keys()).contains(
                "tenant-a/definitions/hybrid-primary.properties",
                "tenant-a/lifecycle/hybrid-primary.state.properties",
                "tenant-a/artifacts/hybrid-primary/package/package/v1/content.bin");
        assertThat(new FileSystemSkillDefinitionStore(fallbackDefinitions).getSkill("hybrid-primary"))
                .isEmpty();
        assertThat(new FileSystemSkillArtifactStore(fallbackArtifacts).getArtifact(primaryArtifact))
                .isEmpty();
    }

    @Test
    void createsConfiguredMirroredCloudPrimaryWithFileBackup(@TempDir Path tempDir) {
        InMemoryPlatformObjectStorageService storage = new InMemoryPlatformObjectStorageService();
        Path backupDefinitions = tempDir.resolve("backup-definitions");
        Path backupLifecycle = tempDir.resolve("backup-lifecycle");
        Path backupEvents = tempDir.resolve("backup-events");
        Path backupArtifacts = tempDir.resolve("backup-artifacts");
        SkillArtifactReference artifactReference =
                SkillArtifactReference.packageArtifact("mirrored-primary", "v1");
        SkillManagementServiceConfig config = SkillManagementServiceConfig.of(
                SkillDefinitionStoreConfig.mirrored(
                        SkillDefinitionStoreConfig.objectStorage("tenant-a/definitions"),
                        SkillDefinitionStoreConfig.fileSystem(backupDefinitions)),
                SkillLifecycleStateStoreConfig.mirrored(
                        SkillLifecycleStateStoreConfig.objectStorage("tenant-a/lifecycle"),
                        SkillLifecycleStateStoreConfig.fileSystem(backupLifecycle)),
                SkillManagementEventStoreConfig.mirrored(
                        SkillManagementEventStoreConfig.objectStorage("tenant-a/events", 50),
                        SkillManagementEventStoreConfig.fileSystem(backupEvents, 50)),
                SkillArtifactStoreConfig.mirrored(
                        SkillArtifactStoreConfig.objectStorage("tenant-a/artifacts"),
                        SkillArtifactStoreConfig.fileSystem(backupArtifacts)),
                SkillLifecycleStateReconcileOptions.inspectOnly());

        SkillManagementService service = SkillManagementServiceRuntimeFactory.create(
                new TestSkillRegistry(),
                config,
                Optional.of(storage),
                Optional.empty());

        service.createSkill(TestSkillDefinitions.basic("mirrored-primary")).await().indefinitely();
        service.disableSkill("mirrored-primary").await().indefinitely();
        service.putArtifact(SkillArtifact.text(artifactReference, "Mirrored skill package."))
                .await()
                .indefinitely();

        assertThat(storage.keys()).contains(
                "tenant-a/definitions/mirrored-primary.properties",
                "tenant-a/lifecycle/mirrored-primary.state.properties",
                "tenant-a/artifacts/mirrored-primary/package/package/v1/content.bin");
        assertThat(storage.keys()).anyMatch(key -> key.startsWith("tenant-a/events/"));
        assertThat(new FileSystemSkillDefinitionStore(backupDefinitions).getSkill("mirrored-primary"))
                .isPresent();
        assertThat(new FileSystemSkillLifecycleStateStore(backupLifecycle)
                .get("mirrored-primary")
                .orElseThrow()
                .status())
                .isEqualTo(SkillLifecycleStatus.DISABLED);
        assertThat(new FileSystemSkillArtifactStore(backupArtifacts).getArtifact(artifactReference))
                .isPresent()
                .get()
                .extracting(artifact -> new String(artifact.content(), StandardCharsets.UTF_8))
                .isEqualTo("Mirrored skill package.");
        assertThat(new FileSystemSkillManagementEventStore(backupEvents).latest().events())
                .extracting(SkillManagementEvent::operation)
                .contains(
                        SkillManagementEventOperation.CREATE_SKILL,
                        SkillManagementEventOperation.TRANSITION_SKILL,
                        SkillManagementEventOperation.PUT_ARTIFACT);
        SkillManagementInspection inspection = service.inspectManagement().await().indefinitely();
        assertThat(inspection.definitionStore().capabilities().names())
                .contains("primary-fallback", "mirror-write");
        assertThat(inspection.eventStore().capabilities().names())
                .contains("primary-fallback", "mirror-write");
    }

    @Test
    void failsFastWhenObjectStorageConfigHasNoRuntimeProvider() {
        SkillManagementServiceConfig config = SkillManagementServiceConfig.of(
                SkillDefinitionStoreConfig.objectStorage("tenant-a/definitions"),
                SkillLifecycleStateStoreConfig.objectStorage("tenant-a/lifecycle"),
                SkillManagementEventStoreConfig.objectStorage("tenant-a/events", 50),
                SkillArtifactStoreConfig.objectStorage("tenant-a/artifacts"),
                SkillLifecycleStateReconcileOptions.inspectOnly());

        assertThatThrownBy(() -> SkillManagementServiceRuntimeFactory.create(
                new TestSkillRegistry(),
                config,
                Optional.empty(),
                Optional.empty()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid skill-management runtime configuration")
                .hasMessageContaining("Skill store kind OBJECT_STORAGE requires dependency: objectStore")
                .hasMessageContaining("Object-storage event store requires a SkillManagementObjectStore")
                .hasMessageContaining("Artifact store kind OBJECT_STORAGE requires dependency: objectStore");
    }

    @Test
    void failsFastWhenJdbcConfigHasNoRuntimeDataSource() {
        SkillManagementServiceConfig config = SkillManagementServiceConfig.of(
                SkillDefinitionStoreConfig.jdbc(),
                SkillLifecycleStateStoreConfig.jdbc(),
                SkillManagementEventStoreConfig.jdbc(),
                SkillArtifactStoreConfig.jdbc(),
                SkillLifecycleStateReconcileOptions.inspectOnly());

        assertThatThrownBy(() -> SkillManagementServiceRuntimeFactory.create(
                new TestSkillRegistry(),
                config,
                Optional.empty(),
                Optional.empty()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid skill-management runtime configuration")
                .hasMessageContaining("Skill store kind JDBC requires dependency: jdbcDataSource")
                .hasMessageContaining("JDBC event store requires a DataSource")
                .hasMessageContaining("Artifact store kind JDBC requires dependency: jdbcDataSource");
    }

    private static final class InMemoryPlatformObjectStorageService implements ObjectStorageService {
        private final Map<String, byte[]> objects = new LinkedHashMap<>();

        @Override
        public Uni<Optional<byte[]>> getObject(String key) {
            return Uni.createFrom().item(Optional.ofNullable(objects.get(key)));
        }

        @Override
        public Uni<List<String>> listObjects(String prefix) {
            String normalizedPrefix = prefix == null ? "" : prefix;
            return Uni.createFrom().item(objects.keySet().stream()
                    .filter(key -> key.startsWith(normalizedPrefix))
                    .toList());
        }

        @Override
        public Uni<Void> putObject(String key, byte[] data) {
            return Uni.createFrom().item(() -> {
                objects.put(key, data);
                return (Void) null;
            });
        }

        @Override
        public Uni<Boolean> deleteObject(String key) {
            return Uni.createFrom().item(() -> objects.remove(key) != null);
        }

        private List<String> keys() {
            return List.copyOf(objects.keySet());
        }
    }
}

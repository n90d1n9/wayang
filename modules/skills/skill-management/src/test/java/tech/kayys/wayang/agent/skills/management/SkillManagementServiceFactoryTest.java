package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tech.kayys.wayang.agent.spi.skills.SkillDefinition;
import tech.kayys.wayang.agent.spi.skills.SkillRegistry;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

class SkillManagementServiceFactoryTest {

    @Test
    void buildsDefaultRegistryBackedService() {
        TestSkillRegistry registry = new TestSkillRegistry();
        SkillManagementService service = new SkillManagementServiceFactory(registry).create();

        service.createSkill(skill("planner")).await().indefinitely();

        assertThat(registry.getSkill("planner")).isPresent();
        assertThat(service.listActiveSkills().await().indefinitely()).extracting(SkillDefinition::id)
                .containsExactly("planner");
    }

    @Test
    void buildsServiceFromDefinitionAndLifecycleStoreConfigs(@TempDir Path tempDir) {
        SkillManagementServiceConfig config = SkillManagementServiceConfig.of(
                SkillDefinitionStoreConfig.fileSystem(tempDir.resolve("definitions")),
                SkillLifecycleStateStoreConfig.fileSystem(tempDir.resolve("lifecycle")));
        SkillManagementServiceFactory factory = new SkillManagementServiceFactory(
                new SkillDefinitionStoreFactory(null),
                new SkillLifecycleStateStoreFactory());

        SkillManagementService firstService = factory.create(config);
        firstService.createSkill(skill("planner")).await().indefinitely();
        firstService.disableSkill("planner").await().indefinitely();

        SkillManagementService secondService = factory.create(config);

        assertThat(secondService.getSkill("planner").await().indefinitely()).isPresent();
        assertThat(secondService.getLifecycleState("planner").await().indefinitely().status())
                .isEqualTo(SkillLifecycleStatus.DISABLED);
    }

    @Test
    void buildsServiceWithCustomLifecycleStateStore() {
        TestSkillRegistry registry = new TestSkillRegistry();
        InMemorySkillLifecycleStateStore lifecycleStore = new InMemorySkillLifecycleStateStore();
        SkillManagementServiceFactory factory = TestSkillManagementFactories.builder(registry)
                .customLifecycleStateStore("redis-lifecycle", lifecycleStore)
                .build();
        SkillManagementServiceConfig config = SkillManagementServiceConfig.of(
                SkillDefinitionStoreConfig.registry(),
                SkillLifecycleStateStoreConfig.custom("redis-lifecycle"));

        SkillManagementService service = factory.create(config);
        service.createSkill(skill("planner")).await().indefinitely();
        service.disableSkill("planner").await().indefinitely();

        assertThat(lifecycleStore.get("planner").orElseThrow().status())
                .isEqualTo(SkillLifecycleStatus.DISABLED);
    }

    @Test
    void buildsServiceWithConfiguredHybridLifecycleStateStore() {
        TestSkillRegistry registry = new TestSkillRegistry();
        InMemorySkillLifecycleStateStore primaryLifecycle = new InMemorySkillLifecycleStateStore();
        InMemorySkillLifecycleStateStore fallbackLifecycle = new InMemorySkillLifecycleStateStore();
        fallbackLifecycle.save(SkillLifecycleState.created("fallback-only"));
        SkillManagementServiceFactory factory = TestSkillManagementFactories.builder(registry)
                .customLifecycleStateStore("primary-lifecycle", primaryLifecycle)
                .customLifecycleStateStore("fallback-lifecycle", fallbackLifecycle)
                .build();
        SkillManagementServiceConfig config = SkillManagementServiceConfig.of(
                SkillDefinitionStoreConfig.registry(),
                SkillLifecycleStateStoreConfig.hybrid(
                        SkillLifecycleStateStoreConfig.custom("primary-lifecycle"),
                        SkillLifecycleStateStoreConfig.custom("fallback-lifecycle")));

        SkillManagementService service = factory.create(config);
        service.createSkill(skill("planner")).await().indefinitely();
        service.disableSkill("planner").await().indefinitely();

        assertThat(primaryLifecycle.get("planner").orElseThrow().status())
                .isEqualTo(SkillLifecycleStatus.DISABLED);
        assertThat(fallbackLifecycle.get("planner")).isEmpty();
        assertThat(service.lifecycleSnapshot().await().indefinitely())
                .containsOnlyKeys("fallback-only", "planner");
    }

    @Test
    void buildsServiceWithConfiguredObjectStorageLifecycleStore() {
        TestSkillRegistry registry = new TestSkillRegistry();
        InMemoryObjectStore objectStore = new InMemoryObjectStore();
        SkillManagementServiceFactory factory = new SkillManagementServiceFactory(registry, objectStore, null);
        SkillManagementServiceConfig config = SkillManagementServiceConfig.of(
                SkillDefinitionStoreConfig.registry(),
                SkillLifecycleStateStoreConfig.objectStorage("tenant-a/lifecycle"));

        SkillManagementService service = factory.create(config);
        service.createSkill(skill("planner")).await().indefinitely();
        service.disableSkill("planner").await().indefinitely();

        ObjectStorageSkillLifecycleStateStore lifecycleStore =
                new ObjectStorageSkillLifecycleStateStore(objectStore, "tenant-a/lifecycle");
        assertThat(lifecycleStore.get("planner").orElseThrow().status())
                .isEqualTo(SkillLifecycleStatus.DISABLED);
        SkillManagementService reloaded = factory.create(config);
        assertThat(reloaded.getLifecycleState("planner").await().indefinitely().status())
                .isEqualTo(SkillLifecycleStatus.DISABLED);
    }

    @Test
    void passesEventSinkToCreatedServices() {
        TestSkillRegistry registry = new TestSkillRegistry();
        InMemorySkillManagementEventSink eventSink = new InMemorySkillManagementEventSink();
        SkillManagementServiceFactory factory = new SkillManagementServiceFactory(
                new SkillDefinitionStoreFactory(registry),
                new SkillLifecycleStateStoreFactory(),
                new SkillDefinitionStoreInspector(),
                new SkillLifecycleStateStoreInspector(),
                eventSink);

        SkillManagementService service = factory.create();
        service.createSkill(skill("planner")).await().indefinitely();

        assertThat(eventSink.events()).hasSize(1);
        assertThat(eventSink.events().get(0).operation()).isEqualTo(SkillManagementEventOperation.CREATE_SKILL);
        assertThat(eventSink.events().get(0).success()).isTrue();
    }

    @Test
    void buildsServiceWithConfiguredFilesystemEventStore(@TempDir Path tempDir) {
        TestSkillRegistry registry = new TestSkillRegistry();
        Path eventDirectory = tempDir.resolve("events");
        SkillManagementServiceFactory factory = new SkillManagementServiceFactory(registry);
        SkillManagementServiceConfig config = SkillManagementServiceConfig.of(
                SkillDefinitionStoreConfig.registry(),
                SkillLifecycleStateStoreConfig.memory(),
                SkillManagementEventStoreConfig.fileSystem(eventDirectory, 10));

        SkillManagementService service = factory.create(config);
        service.createSkill(skill("planner")).await().indefinitely();

        FileSystemSkillManagementEventStore reader = new FileSystemSkillManagementEventStore(eventDirectory);
        assertThat(reader.latest().events()).hasSize(1);
        assertThat(reader.latest().events().get(0).operation())
                .isEqualTo(SkillManagementEventOperation.CREATE_SKILL);
        assertThat(service.eventHistory(SkillManagementEventQuery.latest()).await().indefinitely().events())
                .hasSize(1);
    }

    @Test
    void buildsServiceWithConfiguredObjectStorageEventStore() {
        TestSkillRegistry registry = new TestSkillRegistry();
        InMemoryObjectStore objectStore = new InMemoryObjectStore();
        SkillManagementServiceFactory factory = new SkillManagementServiceFactory(registry, objectStore, null);
        SkillManagementServiceConfig config = SkillManagementServiceConfig.of(
                SkillDefinitionStoreConfig.registry(),
                SkillLifecycleStateStoreConfig.memory(),
                SkillManagementEventStoreConfig.objectStorage("tenant-a/events", 10));

        SkillManagementService service = factory.create(config);
        service.createSkill(skill("planner")).await().indefinitely();

        ObjectStorageSkillManagementEventStore reader =
                new ObjectStorageSkillManagementEventStore(objectStore, "tenant-a/events");
        assertThat(reader.latest().events()).hasSize(1);
        assertThat(reader.latest().events().get(0).operation())
                .isEqualTo(SkillManagementEventOperation.CREATE_SKILL);
        assertThat(service.eventHistory(SkillManagementEventQuery.latest()).await().indefinitely().events())
                .hasSize(1);
    }

    @Test
    void buildsServiceWithConfiguredHybridEventStore(@TempDir Path tempDir) {
        TestSkillRegistry registry = new TestSkillRegistry();
        Path eventDirectory = tempDir.resolve("events");
        SkillManagementServiceFactory factory = new SkillManagementServiceFactory(registry);
        SkillManagementServiceConfig config = SkillManagementServiceConfig.of(
                SkillDefinitionStoreConfig.registry(),
                SkillLifecycleStateStoreConfig.memory(),
                SkillManagementEventStoreConfig.hybrid(
                        SkillManagementEventStoreConfig.memory(10),
                        SkillManagementEventStoreConfig.fileSystem(eventDirectory, 10)));

        SkillManagementService service = factory.create(config);
        service.createSkill(skill("planner")).await().indefinitely();

        FileSystemSkillManagementEventStore fallbackReader = new FileSystemSkillManagementEventStore(eventDirectory);
        assertThat(fallbackReader.latest().events()).hasSize(1);
        assertThat(service.eventHistory(SkillManagementEventQuery.latest()).await().indefinitely().events())
                .hasSize(1);
    }

    @Test
    void buildsServiceWithConfiguredMirroredEventStore() {
        TestSkillRegistry registry = new TestSkillRegistry();
        InMemorySkillManagementEventSink primaryEvents = new InMemorySkillManagementEventSink();
        InMemorySkillManagementEventSink fallbackEvents = new InMemorySkillManagementEventSink();
        SkillManagementServiceFactory factory = TestSkillManagementFactories.builder(registry)
                .customEventStore("primary-events", primaryEvents)
                .customEventStore("fallback-events", fallbackEvents)
                .build();
        SkillManagementServiceConfig config = SkillManagementServiceConfig.of(
                SkillDefinitionStoreConfig.registry(),
                SkillLifecycleStateStoreConfig.memory(),
                SkillManagementEventStoreConfig.mirrored(
                        SkillManagementEventStoreConfig.custom("primary-events"),
                        SkillManagementEventStoreConfig.custom("fallback-events")));

        SkillManagementService service = factory.create(config);
        service.createSkill(skill("planner")).await().indefinitely();

        assertThat(primaryEvents.events()).hasSize(1);
        assertThat(fallbackEvents.events()).hasSize(1);
        assertThat(service.eventHistory(SkillManagementEventQuery.latest()).await().indefinitely().events())
                .hasSize(1);
    }

    @Test
    void buildsServiceWithConfiguredCustomEventStore() {
        TestSkillRegistry registry = new TestSkillRegistry();
        InMemorySkillManagementEventSink eventSink = new InMemorySkillManagementEventSink();
        SkillManagementServiceFactory factory = TestSkillManagementFactories.builder(registry)
                .customEventStore("audit-events", eventSink)
                .build();
        SkillManagementServiceConfig config = SkillManagementServiceConfig.of(
                SkillDefinitionStoreConfig.registry(),
                SkillLifecycleStateStoreConfig.memory(),
                SkillManagementEventStoreConfig.custom("audit-events"));

        SkillManagementService service = factory.create(config);
        service.createSkill(skill("planner")).await().indefinitely();

        assertThat(eventSink.events()).hasSize(1);
        assertThat(eventSink.events().get(0).operation()).isEqualTo(SkillManagementEventOperation.CREATE_SKILL);
        assertThat(service.eventHistory(SkillManagementEventQuery.latest()).await().indefinitely().events())
                .hasSize(1);
    }

    @Test
    void buildsServiceWithConfiguredFilesystemArtifactStore(@TempDir Path tempDir) {
        TestSkillRegistry registry = new TestSkillRegistry();
        Path artifactDirectory = tempDir.resolve("artifacts");
        SkillManagementServiceFactory factory = new SkillManagementServiceFactory(registry);
        SkillManagementServiceConfig config = SkillManagementServiceConfig.of(
                SkillDefinitionStoreConfig.registry(),
                SkillLifecycleStateStoreConfig.memory(),
                SkillManagementEventStoreConfig.none(),
                SkillArtifactStoreConfig.fileSystem(artifactDirectory),
                SkillLifecycleStateReconcileOptions.inspectOnly());
        SkillArtifactReference reference = SkillArtifactReference.packageArtifact("planner", "v1");

        SkillManagementService service = factory.create(config);
        service.putArtifact(SkillArtifact.text(reference, "package")).await().indefinitely();

        SkillManagementService reloaded = factory.create(config);
        assertThat(reloaded.getArtifact(reference).await().indefinitely()).isPresent();
        assertThat(reloaded.listArtifacts("planner").await().indefinitely()).containsExactly(reference);
    }

    @Test
    void buildsServiceWithConfiguredObjectStorageArtifactStore() {
        TestSkillRegistry registry = new TestSkillRegistry();
        InMemoryObjectStore objectStore = new InMemoryObjectStore();
        SkillManagementServiceFactory factory = new SkillManagementServiceFactory(registry, objectStore, null);
        SkillManagementServiceConfig config = SkillManagementServiceConfig.of(
                SkillDefinitionStoreConfig.registry(),
                SkillLifecycleStateStoreConfig.memory(),
                SkillManagementEventStoreConfig.none(),
                SkillArtifactStoreConfig.objectStorage("tenant-a/artifacts"),
                SkillLifecycleStateReconcileOptions.inspectOnly());
        SkillArtifactReference reference = SkillArtifactReference.ragIndex("planner", "kb", "v1");

        SkillManagementService service = factory.create(config);
        service.putArtifact(SkillArtifact.of(reference, new byte[] {1, 2})).await().indefinitely();

        ObjectStorageSkillArtifactStore reader =
                new ObjectStorageSkillArtifactStore(objectStore, "tenant-a/artifacts");
        assertThat(reader.getArtifact(reference)).isPresent();
        assertThat(factory.create(config).listArtifacts("planner").await().indefinitely())
                .containsExactly(reference);
    }

    @Test
    void buildsServiceWithConfiguredJdbcArtifactStore() {
        TestSkillRegistry registry = new TestSkillRegistry();
        SkillManagementServiceFactory factory =
                new SkillManagementServiceFactory(registry, new NoConnectionDataSource());
        SkillManagementServiceConfig config = SkillManagementServiceConfig.of(
                SkillDefinitionStoreConfig.registry(),
                SkillLifecycleStateStoreConfig.memory(),
                SkillManagementEventStoreConfig.none(),
                SkillArtifactStoreConfig.jdbc("skill_artifacts", false),
                SkillLifecycleStateReconcileOptions.inspectOnly());

        assertThat(factory.create(config)).isNotNull();
    }

    @Test
    void buildsServiceWithConfiguredCustomArtifactStore() {
        TestSkillRegistry registry = new TestSkillRegistry();
        InMemorySkillArtifactStore artifacts = new InMemorySkillArtifactStore();
        SkillManagementServiceFactory factory = TestSkillManagementFactories.builder(registry)
                .customArtifactStore("tenant-artifacts", artifacts)
                .build();
        SkillManagementServiceConfig config = SkillManagementServiceConfig.of(
                SkillDefinitionStoreConfig.registry(),
                SkillLifecycleStateStoreConfig.memory(),
                SkillManagementEventStoreConfig.none(),
                SkillArtifactStoreConfig.custom("tenant-artifacts"),
                SkillLifecycleStateReconcileOptions.inspectOnly());
        SkillArtifactReference reference = SkillArtifactReference.mcpDescriptor("planner", "tools", "v1");

        SkillManagementService service = factory.create(config);
        service.putArtifact(SkillArtifact.text(reference, "tools")).await().indefinitely();

        assertThat(artifacts.getArtifact(reference)).isPresent();
        assertThat(service.listArtifacts("planner").await().indefinitely()).containsExactly(reference);
    }

    @Test
    void buildsServiceFromDeploymentConfig(@TempDir Path tempDir) {
        TestSkillRegistry registry = new TestSkillRegistry();
        SkillManagementServiceFactory factory = new SkillManagementServiceFactory(registry);
        SkillManagementServiceConfig serviceConfig = SkillManagementServiceConfig.of(
                SkillDefinitionStoreConfig.fileSystem(tempDir.resolve("deployment-service-definitions")),
                SkillLifecycleStateStoreConfig.fileSystem(tempDir.resolve("deployment-service-lifecycle")),
                SkillManagementEventStoreConfig.none(),
                SkillArtifactStoreConfig.fileSystem(tempDir.resolve("deployment-service-artifacts")),
                SkillLifecycleStateReconcileOptions.inspectOnly());
        SkillManagementDeploymentConfig deploymentConfig = SkillManagementDeploymentConfig.of(
                serviceConfig,
                SkillManagementMaintenanceSourceConfig.of(
                        SkillDefinitionStoreConfig.custom("unused-source-definitions"),
                        SkillArtifactStoreConfig.custom("unused-source-artifacts")),
                SkillManagementMaintenancePlan.inspectOnly());
        SkillArtifactReference reference = SkillArtifactReference.resource("planner", "prompt", "v1");

        SkillManagementService service = factory.create(deploymentConfig);
        service.createSkill(skill("planner")).await().indefinitely();
        service.disableSkill("planner").await().indefinitely();
        service.putArtifact(SkillArtifact.text(reference, "prompt")).await().indefinitely();

        SkillManagementService reloaded = factory.create(deploymentConfig);
        assertThat(reloaded.getSkill("planner").await().indefinitely()).isPresent();
        assertThat(reloaded.getLifecycleState("planner").await().indefinitely().status())
                .isEqualTo(SkillLifecycleStatus.DISABLED);
        assertThat(reloaded.getArtifact(reference).await().indefinitely()).isPresent();
    }

    @Test
    void preflightsDeployableConfigurationWithoutRunningMaintenance() {
        TestSkillRegistry registry = new TestSkillRegistry();
        registry.registerSkill(skill("planner"));
        InMemorySkillArtifactStore sourceArtifacts = new InMemorySkillArtifactStore();
        InMemorySkillManagementEventSink eventSink = new InMemorySkillManagementEventSink();
        SkillManagementServiceFactory factory = TestSkillManagementFactories.builder(registry)
                .customArtifactStore("source-artifacts", sourceArtifacts)
                .eventSink(eventSink)
                .build();
        SkillManagementDeploymentConfig deploymentConfig = SkillManagementDeploymentConfig.of(
                SkillManagementServiceConfig.of(
                        SkillDefinitionStoreConfig.registry(),
                        SkillLifecycleStateStoreConfig.memory(),
                        SkillManagementEventStoreConfig.memory(),
                        SkillArtifactStoreConfig.memory(),
                        SkillLifecycleStateReconcileOptions.inspectOnly()),
                SkillManagementMaintenanceSourceConfig.of(
                        SkillDefinitionStoreConfig.registry(),
                        SkillArtifactStoreConfig.custom("source-artifacts")),
                SkillManagementMaintenancePlan.bootstrap());

        SkillManagementDeploymentPreflightReport report = factory.preflight(deploymentConfig);

        assertThat(report.ready()).isTrue();
        assertThat(report.deployable()).isTrue();
        assertThat(report.errors()).isEmpty();
        assertThat(report.configurationValidation().validConfiguration()).isTrue();
        assertThat(report.targetStoreValidation().validConfiguration()).isTrue();
        assertThat(report.sourceStoreValidation().validConfiguration()).isTrue();
        assertThat(report.capabilityValidation().validConfiguration()).isTrue();
        assertThat(eventSink.events()).isEmpty();
    }

    @Test
    void deploymentPreflightWrapsOperationNeutralValidationReport() {
        SkillManagementPreflightReport validation = new SkillManagementPreflightReport(
                SkillStoreConfigValidationResult.error("configuration failed"),
                null,
                SkillStoreConfigValidationResult.error("source failed"),
                SkillStoreConfigValidationResult.valid());

        SkillManagementDeploymentPreflightReport report = new SkillManagementDeploymentPreflightReport(
                SkillManagementDeploymentConfig.defaults(),
                validation);

        assertThat(validation.ready()).isFalse();
        assertThat(validation.errors()).containsExactly("configuration failed", "source failed");
        assertThat(report.validation()).isEqualTo(validation);
        assertThat(report.ready()).isFalse();
        assertThat(report.errors()).containsExactly("configuration failed", "source failed");
        assertThat(report.errorsMessage()).isEqualTo("configuration failed; source failed");
        assertThat(report.targetStoreValidation().validConfiguration()).isTrue();
    }

    @Test
    void factoryExposesOperationNeutralPreflightValidation() {
        TestSkillRegistry registry = new TestSkillRegistry();
        SkillManagementServiceFactory factory = new SkillManagementServiceFactory(registry);
        SkillManagementServiceConfig serviceConfig = SkillManagementServiceConfig.of(
                SkillDefinitionStoreConfig.registry(),
                SkillLifecycleStateStoreConfig.memory(),
                SkillManagementEventStoreConfig.none(),
                SkillArtifactStoreConfig.memory(),
                SkillLifecycleStateReconcileOptions.inspectOnly());
        SkillManagementMaintenancePlan plan = SkillManagementMaintenancePlan.bootstrap()
                .withEventPruning(SkillManagementEventPruneOptions.keepLatest(10));

        SkillManagementPreflightReport validation = factory.preflightValidation(
                serviceConfig,
                SkillManagementMaintenanceSourceConfig.none(),
                plan);
        SkillManagementDeploymentPreflightReport deploymentReport = factory.preflight(
                serviceConfig,
                SkillManagementMaintenanceSourceConfig.none(),
                plan);

        assertThat(validation.ready()).isFalse();
        assertThat(validation.capabilityValidation().errors()).containsExactly(
                "Event history pruning requires an event store with capability: prune-events");
        assertThat(deploymentReport.validation()).isEqualTo(validation);
    }

    @Test
    void preflightReportsMissingRuntimeDependenciesAcrossTargetAndSourceStores() {
        SkillManagementServiceFactory factory = new SkillManagementServiceFactory((SkillRegistry) null);
        SkillManagementDeploymentConfig deploymentConfig = SkillManagementDeploymentConfig.of(
                SkillManagementServiceConfig.of(
                        SkillDefinitionStoreConfig.objectStorage("target-definitions"),
                        SkillLifecycleStateStoreConfig.jdbc(),
                        SkillManagementEventStoreConfig.objectStorage("target-events"),
                        SkillArtifactStoreConfig.custom("target-artifacts"),
                        SkillLifecycleStateReconcileOptions.inspectOnly()),
                SkillManagementMaintenanceSourceConfig.of(
                        SkillDefinitionStoreConfig.custom("source-definitions"),
                        SkillArtifactStoreConfig.objectStorage("source-artifacts")),
                SkillManagementMaintenancePlan.bootstrap());

        SkillManagementDeploymentPreflightReport report = factory.preflight(deploymentConfig);

        assertThat(report.ready()).isFalse();
        assertThat(report.configurationValidation().validConfiguration()).isTrue();
        assertThat(report.targetStoreValidation().validConfiguration()).isFalse();
        assertThat(report.sourceStoreValidation().validConfiguration()).isFalse();
        assertThat(report.capabilityValidation().validConfiguration()).isTrue();
        assertThat(report.errors())
                .contains(
                        "Skill store kind OBJECT_STORAGE requires dependency: objectStore",
                        "Lifecycle store kind JDBC requires dependency: jdbcDataSource",
                        "Object-storage event store requires a SkillManagementObjectStore",
                        "No custom artifact store registered for: target-artifacts",
                        "No custom skill definition store registered for: source-definitions",
                        "Artifact store kind OBJECT_STORAGE requires dependency: objectStore");
    }

    @Test
    void preflightRejectsEventPrunePlanWithoutPrunableEventStore() {
        TestSkillRegistry registry = new TestSkillRegistry();
        SkillManagementServiceFactory factory = new SkillManagementServiceFactory(registry);
        SkillManagementDeploymentConfig deploymentConfig = SkillManagementDeploymentConfig.of(
                SkillManagementServiceConfig.of(
                        SkillDefinitionStoreConfig.registry(),
                        SkillLifecycleStateStoreConfig.memory(),
                        SkillManagementEventStoreConfig.none(),
                        SkillArtifactStoreConfig.memory(),
                        SkillLifecycleStateReconcileOptions.inspectOnly()),
                SkillManagementMaintenanceSourceConfig.none(),
                SkillManagementMaintenancePlan.bootstrap()
                        .withEventPruning(SkillManagementEventPruneOptions.keepLatest(10)));

        SkillManagementDeploymentPreflightReport report = factory.preflight(deploymentConfig);

        assertThat(report.ready()).isFalse();
        assertThat(report.capabilityValidation().errors()).containsExactly(
                "Event history pruning requires an event store with capability: prune-events");
    }

    @Test
    void preflightRejectsEventPrunePlanForWriteOnlyEventSinkOverride() {
        TestSkillRegistry registry = new TestSkillRegistry();
        SkillManagementEventSink writeOnlySink = event -> {
        };
        SkillManagementServiceFactory factory = TestSkillManagementFactories.builder(registry)
                .eventSink(writeOnlySink)
                .build();
        SkillManagementDeploymentConfig deploymentConfig = SkillManagementDeploymentConfig.of(
                SkillManagementServiceConfig.of(
                        SkillDefinitionStoreConfig.registry(),
                        SkillLifecycleStateStoreConfig.memory(),
                        SkillManagementEventStoreConfig.memory(),
                        SkillArtifactStoreConfig.memory(),
                        SkillLifecycleStateReconcileOptions.inspectOnly()),
                SkillManagementMaintenanceSourceConfig.none(),
                SkillManagementMaintenancePlan.bootstrap()
                        .withEventPruning(SkillManagementEventPruneOptions.keepLatest(10)));

        SkillManagementDeploymentPreflightReport report = factory.preflight(deploymentConfig);

        assertThat(report.ready()).isFalse();
        assertThat(report.capabilityValidation().errors()).containsExactly(
                "Event history pruning requires event sink capability: prune-events");
    }

    @Test
    void preflightRejectsCustomEventStoreThatDisablesPruning() {
        TestSkillRegistry registry = new TestSkillRegistry();
        SkillManagementServiceFactory factory = TestSkillManagementFactories.builder(registry)
                .customEventStore("disabled-events", new DisabledPruningEventStore())
                .build();
        SkillManagementDeploymentConfig deploymentConfig = SkillManagementDeploymentConfig.of(
                SkillManagementServiceConfig.of(
                        SkillDefinitionStoreConfig.registry(),
                        SkillLifecycleStateStoreConfig.memory(),
                        SkillManagementEventStoreConfig.custom("disabled-events"),
                        SkillArtifactStoreConfig.memory(),
                        SkillLifecycleStateReconcileOptions.inspectOnly()),
                SkillManagementMaintenanceSourceConfig.none(),
                SkillManagementMaintenancePlan.bootstrap()
                        .withEventPruning(SkillManagementEventPruneOptions.keepLatest(10)));

        SkillManagementDeploymentPreflightReport report = factory.preflight(deploymentConfig);

        assertThat(report.ready()).isFalse();
        assertThat(report.capabilityValidation().errors()).containsExactly(
                "Custom event store disabled-events requires capability: prune-events");
    }

    @Test
    void preflightAcceptsMirroredEventPrunePlanWithCloudAndFileStores(@TempDir Path tempDir) {
        TestSkillRegistry registry = new TestSkillRegistry();
        InMemoryObjectStore objectStore = new InMemoryObjectStore();
        SkillManagementServiceFactory factory = new SkillManagementServiceFactory(registry, objectStore, null);
        SkillManagementDeploymentConfig deploymentConfig = SkillManagementDeploymentConfig.of(
                SkillManagementServiceConfig.of(
                        SkillDefinitionStoreConfig.registry(),
                        SkillLifecycleStateStoreConfig.memory(),
                        SkillManagementEventStoreConfig.mirrored(
                                SkillManagementEventStoreConfig.objectStorage("tenant-a/events", 50),
                                SkillManagementEventStoreConfig.fileSystem(tempDir.resolve("events"), 50)),
                        SkillArtifactStoreConfig.memory(),
                        SkillLifecycleStateReconcileOptions.inspectOnly()),
                SkillManagementMaintenanceSourceConfig.none(),
                SkillManagementMaintenancePlan.bootstrap()
                        .withEventPruning(SkillManagementEventPruneOptions.keepLatest(10)));

        SkillManagementDeploymentPreflightReport report = factory.preflight(deploymentConfig);

        assertThat(report.ready()).isTrue();
        assertThat(report.deployable()).isTrue();
        assertThat(report.capabilityValidation().validConfiguration()).isTrue();
        assertThat(report.errors()).isEmpty();
    }

    @Test
    void preflightsMaintenanceConfigurationFromParts() {
        TestSkillRegistry registry = new TestSkillRegistry();
        SkillManagementServiceFactory factory = new SkillManagementServiceFactory(registry);

        SkillManagementDeploymentPreflightReport report = factory.preflight(
                SkillManagementServiceConfig.of(
                        SkillDefinitionStoreConfig.registry(),
                        SkillLifecycleStateStoreConfig.memory(),
                        SkillManagementEventStoreConfig.none(),
                        SkillArtifactStoreConfig.memory(),
                        SkillLifecycleStateReconcileOptions.inspectOnly()),
                SkillManagementMaintenanceSourceConfig.none(),
                SkillManagementMaintenancePlan.bootstrap()
                        .withEventPruning(SkillManagementEventPruneOptions.keepLatest(10)));

        assertThat(report.ready()).isFalse();
        assertThat(report.capabilityValidation().errors()).containsExactly(
                "Event history pruning requires an event store with capability: prune-events");
    }

    @Test
    void deployRejectsFailedPreflightBeforeRunningMaintenance() {
        TestSkillRegistry targetRegistry = new TestSkillRegistry();
        SkillDefinition targetSkill = skill("planner");
        targetRegistry.registerSkill(targetSkill);
        InMemorySkillLifecycleStateStore lifecycleStateStore = new InMemorySkillLifecycleStateStore();
        SkillManagementServiceFactory factory = TestSkillManagementFactories.builder(targetRegistry)
                .customLifecycleStateStore("target-lifecycle", lifecycleStateStore)
                .build();
        SkillManagementDeploymentConfig deploymentConfig = SkillManagementDeploymentConfig.of(
                SkillManagementServiceConfig.of(
                        SkillDefinitionStoreConfig.registry(),
                        SkillLifecycleStateStoreConfig.custom("target-lifecycle"),
                        SkillManagementEventStoreConfig.none(),
                        SkillArtifactStoreConfig.memory(),
                        SkillLifecycleStateReconcileOptions.inspectOnly()),
                SkillManagementMaintenanceSourceConfig.none(),
                SkillManagementMaintenancePlan.bootstrap()
                        .withEventPruning(SkillManagementEventPruneOptions.keepLatest(10)));

        Throwable thrown = catchThrowable(() -> factory.deploy(deploymentConfig));

        assertThat(thrown).isInstanceOf(SkillManagementDeploymentPreflightException.class)
                .hasMessageContaining("Skill-management deployment preflight failed")
                .hasMessageContaining("Event history pruning requires an event store with capability: prune-events");
        SkillManagementDeploymentPreflightException error =
                (SkillManagementDeploymentPreflightException) thrown;
        assertThat(error.operation()).isEqualTo(SkillManagementEventOperation.DEPLOYMENT);
        assertThat(error.preflightReport()).isEqualTo(error.report().validation());
        assertThat(error.report().capabilityValidation().errors()).containsExactly(
                "Event history pruning requires an event store with capability: prune-events");
        assertThat(targetRegistry.getSkill("planner")).contains(targetSkill);
        assertThat(lifecycleStateStore.snapshot()).isEmpty();
    }

    @Test
    void runMaintenanceRejectsFailedPreflightBeforeRunningMaintenance() {
        TestSkillRegistry targetRegistry = new TestSkillRegistry();
        SkillDefinition targetSkill = skill("planner");
        targetRegistry.registerSkill(targetSkill);
        InMemorySkillLifecycleStateStore lifecycleStateStore = new InMemorySkillLifecycleStateStore();
        SkillManagementServiceFactory factory = TestSkillManagementFactories.builder(targetRegistry)
                .customLifecycleStateStore("target-lifecycle", lifecycleStateStore)
                .build();
        SkillManagementServiceConfig targetConfig = SkillManagementServiceConfig.of(
                SkillDefinitionStoreConfig.registry(),
                SkillLifecycleStateStoreConfig.custom("target-lifecycle"),
                SkillManagementEventStoreConfig.none(),
                SkillArtifactStoreConfig.memory(),
                SkillLifecycleStateReconcileOptions.inspectOnly());

        Throwable thrown = catchThrowable(() -> factory.runMaintenance(
                targetConfig,
                SkillManagementMaintenanceSourceConfig.none(),
                SkillManagementMaintenancePlan.bootstrap()
                        .withEventPruning(SkillManagementEventPruneOptions.keepLatest(10))));

        assertThat(thrown).isInstanceOf(SkillManagementMaintenancePreflightException.class)
                .hasMessageContaining("Skill-management maintenance preflight failed")
                .hasMessageContaining("Event history pruning requires an event store with capability: prune-events");
        SkillManagementMaintenancePreflightException error =
                (SkillManagementMaintenancePreflightException) thrown;
        assertThat(error.operation()).isEqualTo(SkillManagementEventOperation.MAINTENANCE);
        assertThat(error.preflightReport().capabilityValidation().errors()).containsExactly(
                "Event history pruning requires an event store with capability: prune-events");
        assertThat(targetRegistry.getSkill("planner")).contains(targetSkill);
        assertThat(lifecycleStateStore.snapshot()).isEmpty();
    }

    @Test
    void runMaintenanceEmitsFailureEventWhenPreflightFailsWithEventSinkOverride() {
        TestSkillRegistry registry = new TestSkillRegistry();
        InMemorySkillManagementEventSink eventSink = new InMemorySkillManagementEventSink();
        SkillManagementServiceFactory factory = TestSkillManagementFactories.builder(registry)
                .eventSink(eventSink)
                .build();

        assertThatThrownBy(() -> factory.runMaintenance(
                SkillManagementServiceConfig.of(
                        SkillDefinitionStoreConfig.registry(),
                        SkillLifecycleStateStoreConfig.memory(),
                        SkillManagementEventStoreConfig.none(),
                        SkillArtifactStoreConfig.memory(),
                        SkillLifecycleStateReconcileOptions.inspectOnly()),
                SkillManagementMaintenanceSourceConfig.definitions(
                        SkillDefinitionStoreConfig.custom("missing-source")),
                SkillManagementMaintenancePlan.bootstrap()))
                .isInstanceOf(SkillManagementMaintenancePreflightException.class)
                .hasMessageContaining("No custom skill definition store registered for: missing-source");

        assertThat(eventSink.events()).hasSize(1);
        SkillManagementEvent maintenanceEvent = eventSink.events().get(0);
        assertThat(maintenanceEvent.operation()).isEqualTo(SkillManagementEventOperation.MAINTENANCE);
        assertThat(maintenanceEvent.success()).isFalse();
        assertThat(maintenanceEvent.attributes())
                .containsEntry("errorType", "SkillManagementMaintenancePreflightException")
                .containsEntry("preflightReady", "false")
                .containsEntry("preflightDeployable", "false")
                .containsEntry("preflightErrors", "1")
                .containsEntry("preflightSourceStoreErrors", "1")
                .containsEntry(
                        "preflightSourceStoreMessage",
                        "No custom skill definition store registered for: missing-source")
                .containsEntry("preflightCapabilityErrors", "0")
                .containsEntry(
                        "error",
                        "Skill-management maintenance preflight failed: "
                                + "No custom skill definition store registered for: missing-source");
    }

    @Test
    void runsConfiguredMaintenanceFromSourceStores(@TempDir Path tempDir) {
        TestSkillRegistry targetRegistry = new TestSkillRegistry();
        TestSkillRegistry sourceRegistry = new TestSkillRegistry();
        SkillDefinition sourceSkill = skill("planner");
        sourceRegistry.registerSkill(sourceSkill);
        InMemorySkillArtifactStore sourceArtifacts = new InMemorySkillArtifactStore();
        SkillArtifactReference reference = SkillArtifactReference.packageArtifact("planner", "v1");
        sourceArtifacts.putArtifact(SkillArtifact.text(reference, "package"));
        SkillManagementServiceFactory factory = TestSkillManagementFactories.builder(targetRegistry)
                .customDefinitionStore("source-definitions", new RegistrySkillDefinitionStore(sourceRegistry))
                .customArtifactStore("source-artifacts", sourceArtifacts)
                .build();
        SkillManagementServiceConfig targetConfig = SkillManagementServiceConfig.of(
                SkillDefinitionStoreConfig.fileSystem(tempDir.resolve("target-definitions")),
                SkillLifecycleStateStoreConfig.memory(),
                SkillManagementEventStoreConfig.none(),
                SkillArtifactStoreConfig.fileSystem(tempDir.resolve("target-artifacts")),
                SkillLifecycleStateReconcileOptions.inspectOnly());
        SkillManagementMaintenanceSourceConfig sourceConfig = SkillManagementMaintenanceSourceConfig.of(
                SkillDefinitionStoreConfig.custom("source-definitions"),
                SkillArtifactStoreConfig.custom("source-artifacts"));

        SkillManagementMaintenanceResult result =
                factory.runMaintenance(targetConfig, sourceConfig, SkillManagementMaintenancePlan.bootstrap());

        assertThat(result.definitionSyncResult().copied()).isEqualTo(1);
        assertThat(result.artifactSyncResult().copied()).isEqualTo(1);
        SkillManagementService targetService = factory.create(targetConfig);
        assertThat(targetService.getSkill("planner").await().indefinitely()).contains(sourceSkill);
        assertThat(targetService.getArtifact(reference).await().indefinitely()).isPresent();
    }

    @Test
    void runsMaintenanceFromDeploymentConfig(@TempDir Path tempDir) {
        TestSkillRegistry targetRegistry = new TestSkillRegistry();
        TestSkillRegistry sourceRegistry = new TestSkillRegistry();
        SkillDefinition sourceSkill = skill("planner");
        sourceRegistry.registerSkill(sourceSkill);
        InMemorySkillArtifactStore sourceArtifacts = new InMemorySkillArtifactStore();
        SkillArtifactReference reference = SkillArtifactReference.ragIndex("planner", "kb", "v1");
        sourceArtifacts.putArtifact(SkillArtifact.text(reference, "index"));
        SkillManagementServiceFactory factory = TestSkillManagementFactories.builder(targetRegistry)
                .customDefinitionStore("source-definitions", new RegistrySkillDefinitionStore(sourceRegistry))
                .customArtifactStore("source-artifacts", sourceArtifacts)
                .build();
        SkillManagementServiceConfig targetConfig = SkillManagementServiceConfig.of(
                SkillDefinitionStoreConfig.fileSystem(tempDir.resolve("deployment-definitions")),
                SkillLifecycleStateStoreConfig.memory(),
                SkillManagementEventStoreConfig.none(),
                SkillArtifactStoreConfig.fileSystem(tempDir.resolve("deployment-artifacts")),
                SkillLifecycleStateReconcileOptions.inspectOnly());
        SkillManagementDeploymentConfig deploymentConfig = SkillManagementDeploymentConfig.of(
                targetConfig,
                SkillManagementMaintenanceSourceConfig.of(
                        SkillDefinitionStoreConfig.custom("source-definitions"),
                        SkillArtifactStoreConfig.custom("source-artifacts")),
                SkillManagementMaintenancePlan.bootstrap());

        SkillManagementMaintenanceResult result = factory.runMaintenance(deploymentConfig);

        assertThat(result.definitionSyncResult().copied()).isEqualTo(1);
        assertThat(result.artifactSyncResult().copied()).isEqualTo(1);
        SkillManagementService targetService = factory.create(targetConfig);
        assertThat(targetService.getSkill("planner").await().indefinitely()).contains(sourceSkill);
        assertThat(targetService.getArtifact(reference).await().indefinitely()).isPresent();
    }

    @Test
    void deploysWithSharedManagedStoresForMemoryTargets() {
        TestSkillRegistry targetRegistry = new TestSkillRegistry();
        TestSkillRegistry sourceRegistry = new TestSkillRegistry();
        SkillDefinition sourceSkill = skill("planner");
        sourceRegistry.registerSkill(sourceSkill);
        InMemorySkillArtifactStore sourceArtifacts = new InMemorySkillArtifactStore();
        SkillArtifactReference reference = SkillArtifactReference.packageArtifact("planner", "v1");
        sourceArtifacts.putArtifact(SkillArtifact.text(reference, "package"));
        SkillManagementServiceFactory factory = TestSkillManagementFactories.builder(targetRegistry)
                .customDefinitionStore("source-definitions", new RegistrySkillDefinitionStore(sourceRegistry))
                .customArtifactStore("source-artifacts", sourceArtifacts)
                .build();
        SkillManagementDeploymentConfig deploymentConfig = SkillManagementDeploymentConfig.of(
                SkillManagementServiceConfig.of(
                        SkillDefinitionStoreConfig.registry(),
                        SkillLifecycleStateStoreConfig.memory(),
                        SkillManagementEventStoreConfig.none(),
                        SkillArtifactStoreConfig.memory(),
                        SkillLifecycleStateReconcileOptions.inspectOnly()),
                SkillManagementMaintenanceSourceConfig.of(
                        SkillDefinitionStoreConfig.custom("source-definitions"),
                        SkillArtifactStoreConfig.custom("source-artifacts")),
                SkillManagementMaintenancePlan.bootstrap());

        SkillManagementDeploymentResult result = factory.deploy(deploymentConfig);

        assertThat(result.changed()).isTrue();
        assertThat(result.consistent()).isTrue();
        assertThat(result.maintenanceResult().definitionSyncResult().copied()).isEqualTo(1);
        assertThat(result.maintenanceResult().artifactSyncResult().copied()).isEqualTo(1);
        assertThat(result.service().getSkill("planner").await().indefinitely()).contains(sourceSkill);
        assertThat(result.service().getLifecycleState("planner").await().indefinitely().status())
                .isEqualTo(SkillLifecycleStatus.ACTIVE);
        assertThat(result.service().getArtifact(reference).await().indefinitely()).isPresent();
    }

    @Test
    void emitsEventForDeployment() {
        TestSkillRegistry registry = new TestSkillRegistry();
        registry.registerSkill(skill("planner"));
        InMemorySkillManagementEventSink eventSink = new InMemorySkillManagementEventSink();
        SkillManagementServiceFactory factory = TestSkillManagementFactories.builder(registry)
                .eventSink(eventSink)
                .build();

        SkillManagementDeploymentResult result = factory.deploy(SkillManagementDeploymentConfig.of(
                SkillManagementServiceConfig.of(
                        SkillDefinitionStoreConfig.registry(),
                        SkillLifecycleStateStoreConfig.memory(),
                        SkillManagementEventStoreConfig.none(),
                        SkillArtifactStoreConfig.memory(),
                        SkillLifecycleStateReconcileOptions.inspectOnly()),
                SkillManagementMaintenanceSourceConfig.none(),
                SkillManagementMaintenancePlan.bootstrap()));

        assertThat(result.changed()).isTrue();
        assertThat(eventSink.events()).extracting(SkillManagementEvent::operation)
                .containsExactly(
                        SkillManagementEventOperation.MAINTENANCE,
                        SkillManagementEventOperation.DEPLOYMENT);
        SkillManagementEvent maintenanceEvent = eventSink.events().get(0);
        SkillManagementEvent deploymentEvent = eventSink.events().get(1);
        assertThat(deploymentEvent.success()).isTrue();
        assertThat(deploymentEvent.attributes())
                .containsEntry("dryRun", "false")
                .containsEntry("changed", "true")
                .containsEntry("consistent", "true")
                .containsEntry("definitionChanges", "0")
                .containsEntry("artifactChanges", "0")
                .containsEntry("lifecycleCreated", "1")
                .containsEntry("lifecycleRemoved", "0");
        assertThat(deploymentEvent.attributes().get("operationId")).isNotBlank();
        assertThat(maintenanceEvent.attributes().get("operationId")).isNotBlank();
        assertThat(maintenanceEvent.attributes().get("operationId"))
                .isNotEqualTo(deploymentEvent.attributes().get("operationId"));
        assertThat(maintenanceEvent.attributes())
                .containsEntry("parentOperationId", deploymentEvent.attributes().get("operationId"));
    }

    @Test
    void emitsFailureEventWhenDeploymentFails() {
        TestSkillRegistry registry = new TestSkillRegistry();
        InMemorySkillManagementEventSink eventSink = new InMemorySkillManagementEventSink();
        SkillManagementServiceFactory factory = TestSkillManagementFactories.builder(registry)
                .eventSink(eventSink)
                .build();

        assertThatThrownBy(() -> factory.deploy(SkillManagementDeploymentConfig.of(
                SkillManagementServiceConfig.of(
                        SkillDefinitionStoreConfig.registry(),
                        SkillLifecycleStateStoreConfig.memory(),
                        SkillManagementEventStoreConfig.none(),
                        SkillArtifactStoreConfig.memory(),
                        SkillLifecycleStateReconcileOptions.inspectOnly()),
                SkillManagementMaintenanceSourceConfig.definitions(
                        SkillDefinitionStoreConfig.custom("missing-source")),
                SkillManagementMaintenancePlan.bootstrap())))
                .isInstanceOf(SkillManagementDeploymentPreflightException.class)
                .hasMessageContaining("No custom skill definition store registered for: missing-source");

        assertThat(eventSink.events()).hasSize(1);
        SkillManagementEvent deploymentEvent = eventSink.events().get(0);
        assertThat(deploymentEvent.operation()).isEqualTo(SkillManagementEventOperation.DEPLOYMENT);
        assertThat(deploymentEvent.success()).isFalse();
        assertThat(deploymentEvent.attributes())
                .containsEntry("errorType", "SkillManagementDeploymentPreflightException")
                .containsEntry("preflightReady", "false")
                .containsEntry("preflightDeployable", "false")
                .containsEntry("preflightErrors", "1")
                .containsEntry("preflightSourceStoreErrors", "1")
                .containsEntry(
                        "preflightSourceStoreMessage",
                        "No custom skill definition store registered for: missing-source")
                .containsEntry("preflightCapabilityErrors", "0")
                .containsEntry(
                        "error",
                        "Skill-management deployment preflight failed: "
                                + "No custom skill definition store registered for: missing-source");
    }

    @Test
    void runMaintenanceWithoutSourceUsesManagedStoresAsSafeSource() {
        TestSkillRegistry registry = new TestSkillRegistry();
        SkillDefinition skill = skill("planner");
        registry.registerSkill(skill);
        InMemorySkillArtifactStore artifacts = new InMemorySkillArtifactStore();
        SkillArtifactReference reference = SkillArtifactReference.resource("planner", "prompt", "v1");
        artifacts.putArtifact(SkillArtifact.text(reference, "prompt"));
        SkillManagementServiceFactory factory = TestSkillManagementFactories.builder(registry)
                .customArtifactStore("target-artifacts", artifacts)
                .build();
        SkillManagementServiceConfig config = SkillManagementServiceConfig.of(
                SkillDefinitionStoreConfig.registry(),
                SkillLifecycleStateStoreConfig.memory(),
                SkillManagementEventStoreConfig.none(),
                SkillArtifactStoreConfig.custom("target-artifacts"),
                SkillLifecycleStateReconcileOptions.inspectOnly());

        SkillManagementMaintenanceResult result =
                factory.runMaintenance(config, null, SkillManagementMaintenancePlan.mirrorAndRepair());

        assertThat(result.definitionSyncResult().deleted()).isZero();
        assertThat(result.definitionSyncResult().unchanged()).isEqualTo(1);
        assertThat(result.artifactSyncResult().deleted()).isZero();
        assertThat(result.artifactSyncResult().unchanged()).isEqualTo(1);
        assertThat(registry.getSkill("planner")).contains(skill);
        assertThat(artifacts.getArtifact(reference)).isPresent();
    }

    private SkillDefinition skill(String id) {
        return TestSkillDefinitions.basic(id);
    }

    private static final class InMemoryObjectStore implements SkillManagementObjectStore {
        private final Map<String, byte[]> objects = new LinkedHashMap<>();

        @Override
        public Optional<byte[]> get(String key) {
            return Optional.ofNullable(objects.get(key));
        }

        @Override
        public List<String> list(String prefix) {
            String normalizedPrefix = prefix == null ? "" : prefix;
            return objects.keySet().stream()
                    .filter(key -> key.startsWith(normalizedPrefix))
                    .toList();
        }

        @Override
        public void put(String key, byte[] content) {
            objects.put(key, content);
        }

        @Override
        public boolean delete(String key) {
            return objects.remove(key) != null;
        }
    }

    private static final class DisabledPruningEventStore implements SkillManagementEventSink, SkillManagementEventPruner {

        @Override
        public void record(SkillManagementEvent event) {
        }

        @Override
        public SkillManagementEventPruneResult pruneEvents(SkillManagementEventPruneOptions options) {
            return SkillManagementEventPruneResult.failure(options, "disabled");
        }

        @Override
        public boolean supportsPruning() {
            return false;
        }
    }

    private static final class NoConnectionDataSource implements DataSource {

        @Override
        public Connection getConnection() throws SQLException {
            throw new SQLException("No connection expected");
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            return getConnection();
        }

        @Override
        public PrintWriter getLogWriter() {
            return null;
        }

        @Override
        public void setLogWriter(PrintWriter out) {
        }

        @Override
        public void setLoginTimeout(int seconds) {
        }

        @Override
        public int getLoginTimeout() {
            return 0;
        }

        @Override
        public Logger getParentLogger() throws SQLFeatureNotSupportedException {
            throw new SQLFeatureNotSupportedException();
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            throw new SQLException("unwrap is not supported");
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) {
            return false;
        }
    }
}

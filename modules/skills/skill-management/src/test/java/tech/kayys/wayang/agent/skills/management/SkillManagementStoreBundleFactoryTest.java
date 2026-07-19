package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.sql.DataSource;
import java.util.Map;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class SkillManagementStoreBundleFactoryTest {

    @Test
    void eventSinkOverrideSkipsConfiguredEventStoreDependencyValidation(@TempDir Path tempDir) {
        InMemorySkillManagementEventSink eventSink = new InMemorySkillManagementEventSink();
        SkillManagementStoreBundleFactory factory = newFactory(eventSink);
        SkillManagementServiceConfig config = serviceConfig(
                tempDir,
                SkillManagementEventStoreConfig.objectStorage("tenant-a/events"));

        SkillStoreConfigValidationResult validation = factory.validateManagedStores(config);
        SkillManagementStoreBundle bundle = factory.create(config);

        assertThat(validation.validConfiguration()).isTrue();
        assertThat(bundle.eventSink()).isSameAs(eventSink);
    }

    @Test
    void maintenanceSourcesFallbackToManagedTargetStores(@TempDir Path tempDir) {
        SkillManagementStoreBundleFactory factory = newFactory(null);
        SkillManagementStoreBundle bundle = factory.create(serviceConfig(
                tempDir,
                SkillManagementEventStoreConfig.none()));
        SkillManagementMaintenanceStores maintenanceStores = factory.maintenanceStores(null, bundle);

        assertThat(maintenanceStores.sourceDefinitions()).isSameAs(bundle.definitionStore());
        assertThat(maintenanceStores.targetDefinitions()).isSameAs(bundle.definitionStore());
        assertThat(maintenanceStores.lifecycleStateStore()).isSameAs(bundle.lifecycleStateStore());
        assertThat(maintenanceStores.sourceArtifacts()).isSameAs(bundle.artifactStore());
        assertThat(maintenanceStores.targetArtifacts()).isSameAs(bundle.artifactStore());
        assertThat(maintenanceStores.eventSink()).isSameAs(bundle.eventSink());
    }

    @Test
    void maintenanceStoresUseConfiguredSourceStores(@TempDir Path tempDir) {
        TestSkillDefinitionStore sourceDefinitions = new TestSkillDefinitionStore();
        InMemorySkillArtifactStore sourceArtifacts = new InMemorySkillArtifactStore();
        SkillManagementStoreBundleFactory factory = newFactory(
                null,
                Map.of("source-definitions", sourceDefinitions),
                Map.of("source-artifacts", sourceArtifacts));
        SkillManagementStoreBundle targetStores = factory.create(serviceConfig(
                tempDir,
                SkillManagementEventStoreConfig.none()));

        SkillManagementMaintenanceStores maintenanceStores = factory.maintenanceStores(
                SkillManagementMaintenanceSourceConfig.of(
                        SkillDefinitionStoreConfig.custom("source-definitions"),
                        SkillArtifactStoreConfig.custom("source-artifacts")),
                targetStores);

        assertThat(maintenanceStores.sourceDefinitions()).isSameAs(sourceDefinitions);
        assertThat(maintenanceStores.sourceArtifacts()).isSameAs(sourceArtifacts);
        assertThat(maintenanceStores.targetDefinitions()).isSameAs(targetStores.definitionStore());
        assertThat(maintenanceStores.targetArtifacts()).isSameAs(targetStores.artifactStore());
    }

    @Test
    void eventSinkOverrideCapabilityValidationUsesEffectiveSinkCapability(@TempDir Path tempDir) {
        SkillManagementEventSink writeOnlySink = event -> {
        };
        SkillManagementStoreBundleFactory factory = newFactory(writeOnlySink);

        SkillStoreConfigValidationResult validation = factory.validatePlanCapabilities(
                serviceConfig(tempDir, SkillManagementEventStoreConfig.memory()),
                SkillManagementMaintenancePlan.bootstrap()
                        .withEventPruning(SkillManagementEventPruneOptions.keepLatest(1)));

        assertThat(validation.errors())
                .containsExactly(SkillManagementEventPruner.PRUNE_EVENTS_CAPABILITY_REQUIRED);
    }

    private SkillManagementStoreBundleFactory newFactory(SkillManagementEventSink eventSinkOverride) {
        return newFactory(eventSinkOverride, Map.of(), Map.of());
    }

    private SkillManagementStoreBundleFactory newFactory(
            SkillManagementEventSink eventSinkOverride,
            Map<String, SkillDefinitionStore> definitionStores,
            Map<String, SkillArtifactStore> artifactStores) {
        return new SkillManagementStoreBundleFactory(
                new SkillDefinitionStoreFactory(
                        null,
                        (SkillManagementObjectStore) null,
                        (DataSource) null,
                        definitionStores),
                new SkillLifecycleStateStoreFactory(),
                new SkillManagementEventStoreFactory(),
                new SkillArtifactStoreFactory(artifactStores),
                eventSinkOverride);
    }

    private SkillManagementServiceConfig serviceConfig(
            Path tempDir,
            SkillManagementEventStoreConfig eventStore) {
        return SkillManagementServiceConfig.of(
                SkillDefinitionStoreConfig.fileSystem(tempDir.resolve("definitions")),
                SkillLifecycleStateStoreConfig.memory(),
                eventStore,
                SkillArtifactStoreConfig.memory(),
                SkillLifecycleStateReconcileOptions.inspectOnly());
    }

}

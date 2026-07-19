package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class SkillManagementPreflightServiceTest {

    @Test
    void buildsReadyDeploymentPreflightForValidStores(@TempDir Path tempDir) {
        SkillManagementPreflightService preflightService = newPreflightService(null);
        SkillManagementDeploymentConfig config = SkillManagementDeploymentConfig.of(
                serviceConfig(tempDir, SkillManagementEventStoreConfig.memory()),
                SkillManagementMaintenanceSourceConfig.none(),
                SkillManagementMaintenancePlan.bootstrap());

        SkillManagementDeploymentPreflightReport report = preflightService.preflight(config);

        assertThat(report.ready()).isTrue();
        assertThat(report.deployable()).isTrue();
        assertThat(report.validation().ready()).isTrue();
        assertThat(report.errors()).isEmpty();
    }

    @Test
    void separatesConfigurationSourceAndCapabilityValidationBuckets(@TempDir Path tempDir) {
        SkillManagementPreflightService preflightService = newPreflightService(null);
        SkillManagementServiceConfig serviceConfig = serviceConfig(
                tempDir,
                SkillManagementEventStoreConfig.none());
        SkillManagementMaintenanceSourceConfig sourceConfig =
                SkillManagementMaintenanceSourceConfig.definitions(
                        SkillDefinitionStoreConfig.custom("missing-source"));
        SkillManagementMaintenancePlan plan = SkillManagementMaintenancePlan.bootstrap()
                .withEventPruning(SkillManagementEventPruneOptions.keepLatest(1));

        SkillManagementPreflightReport validation = preflightService.validation(
                serviceConfig,
                sourceConfig,
                plan);
        SkillManagementDeploymentPreflightReport report = preflightService.preflight(
                serviceConfig,
                sourceConfig,
                plan);

        assertThat(validation.ready()).isFalse();
        assertThat(validation.configurationValidation().validConfiguration()).isTrue();
        assertThat(validation.targetStoreValidation().validConfiguration()).isTrue();
        assertThat(validation.sourceStoreValidation().errors())
                .containsExactly("No custom skill definition store registered for: missing-source");
        assertThat(validation.capabilityValidation().errors())
                .containsExactly("Event history pruning requires an event store with capability: prune-events");
        assertThat(report.validation()).isEqualTo(validation);
    }

    @Test
    void validatesPruningAgainstEventSinkOverride(@TempDir Path tempDir) {
        SkillManagementEventSink writeOnlySink = event -> {
        };
        SkillManagementPreflightService preflightService = newPreflightService(writeOnlySink);

        SkillManagementPreflightReport validation = preflightService.validation(
                serviceConfig(tempDir, SkillManagementEventStoreConfig.memory()),
                SkillManagementMaintenanceSourceConfig.none(),
                SkillManagementMaintenancePlan.bootstrap()
                        .withEventPruning(SkillManagementEventPruneOptions.keepLatest(1)));

        assertThat(validation.capabilityValidation().errors())
                .containsExactly(SkillManagementEventPruner.PRUNE_EVENTS_CAPABILITY_REQUIRED);
    }

    private SkillManagementPreflightService newPreflightService(SkillManagementEventSink eventSinkOverride) {
        return new SkillManagementPreflightService(new SkillManagementStoreBundleFactory(
                new SkillDefinitionStoreFactory(null),
                new SkillLifecycleStateStoreFactory(),
                new SkillManagementEventStoreFactory(),
                new SkillArtifactStoreFactory(),
                eventSinkOverride));
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

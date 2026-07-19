package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class SkillManagementPreflightReportFactoryTest {

    @Test
    void deploymentReportUsesSameValidationAssemblyAsNeutralReport(@TempDir Path tempDir) {
        SkillManagementPreflightReportFactory factory = newFactory();
        SkillManagementDeploymentConfig config = SkillManagementDeploymentConfig.of(
                serviceConfig(tempDir, SkillManagementEventStoreConfig.none()),
                SkillManagementMaintenanceSourceConfig.definitions(
                        SkillDefinitionStoreConfig.custom("missing-source")),
                SkillManagementMaintenancePlan.bootstrap()
                        .withEventPruning(SkillManagementEventPruneOptions.keepLatest(1)));

        SkillManagementDeploymentPreflightReport deploymentReport = factory.preflight(config);
        SkillManagementPreflightReport validation = factory.validation(config);

        assertThat(deploymentReport.validation()).isEqualTo(validation);
        assertThat(deploymentReport.ready()).isFalse();
        assertThat(validation.sourceStoreValidation().errors())
                .containsExactly("No custom skill definition store registered for: missing-source");
        assertThat(validation.capabilityValidation().errors())
                .containsExactly("Event history pruning requires an event store with capability: prune-events");
    }

    private SkillManagementPreflightReportFactory newFactory() {
        return new SkillManagementPreflightReportFactory(new SkillManagementStoreBundleFactory(
                new SkillDefinitionStoreFactory(null),
                new SkillLifecycleStateStoreFactory(),
                new SkillManagementEventStoreFactory(),
                new SkillArtifactStoreFactory(),
                null));
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

package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class SkillManagementPreflightMatrixTest {

    @Test
    void joinsValidationBucketsWithTargetSourceAndCapabilityRows(@TempDir Path tempDir) {
        SkillManagementPreflightService preflightService = newPreflightService(null);
        SkillManagementDeploymentPreflightReport report = preflightService.preflight(
                SkillManagementDeploymentConfig.of(
                        serviceConfig(tempDir, SkillManagementEventStoreConfig.none()),
                        SkillManagementMaintenanceSourceConfig.definitions(
                                SkillDefinitionStoreConfig.custom("missing-source")),
                        SkillManagementMaintenancePlan.bootstrap()
                                .withEventPruning(SkillManagementEventPruneOptions.keepLatest(1))));

        SkillManagementPreflightMatrix matrix = report.matrix();

        assertThat(matrix.ready()).isFalse();
        assertThat(matrix.errors()).containsExactly(
                "No custom skill definition store registered for: missing-source",
                "Event history pruning requires an event store with capability: prune-events");
        assertThat(matrix.failedRows())
                .extracting(SkillManagementPreflightMatrix.Row::path)
                .containsExactly("source-stores", "capabilities", "capability.event-pruning");
        assertThat(matrix.rows(SkillManagementPreflightMatrix.Scope.TARGET_STORE))
                .extracting(SkillManagementPreflightMatrix.Row::path)
                .contains("target-stores", "target.definition", "target.event-history");

        SkillManagementPreflightMatrix.Row sourceDefinition = matrix.rows().stream()
                .filter(row -> row.path().equals("source.definition"))
                .findFirst()
                .orElseThrow();
        assertThat(sourceDefinition.required()).isTrue();
        assertThat(sourceDefinition.provider()).isEqualTo("custom");
        assertThat(sourceDefinition.custom()).isTrue();

        SkillManagementPreflightMatrix.Row eventPrune = matrix.rows().stream()
                .filter(row -> row.path().equals("capability.event-pruning"))
                .findFirst()
                .orElseThrow();
        assertThat(eventPrune.required()).isTrue();
        assertThat(eventPrune.valid()).isFalse();
        assertThat(eventPrune.provider()).isEqualTo("none");
        assertThat(eventPrune.capabilities()).isEmpty();
        assertThat(eventPrune.errors()).containsExactly(
                "Event history pruning requires an event store with capability: prune-events");
    }

    @Test
    void factoryBuildsReadyPreflightMatrix(@TempDir Path tempDir) {
        SkillManagementServiceFactory factory = new SkillManagementServiceFactory(new TestSkillRegistry());

        SkillManagementPreflightMatrix matrix = factory.preflightMatrix(
                SkillManagementDeploymentConfig.of(
                        serviceConfig(tempDir, SkillManagementEventStoreConfig.memory()),
                        SkillManagementMaintenanceSourceConfig.none(),
                        SkillManagementMaintenancePlan.bootstrap()));

        assertThat(matrix.ready()).isTrue();
        assertThat(matrix.failedRows()).isEmpty();
        assertThat(matrix.rows(SkillManagementPreflightMatrix.Scope.CAPABILITY))
                .extracting(SkillManagementPreflightMatrix.Row::path)
                .containsExactly("capabilities", "capability.event-pruning");
        SkillManagementPreflightMatrix.Row eventPrune = matrix.rows().stream()
                .filter(row -> row.path().equals("capability.event-pruning"))
                .findFirst()
                .orElseThrow();
        assertThat(eventPrune.required()).isFalse();
        assertThat(eventPrune.valid()).isTrue();
        assertThat(eventPrune.capabilities()).contains("write", "query-events", "prune-events");
    }

    @Test
    void neutralPreflightReportCanBuildMatrixFromConfig(@TempDir Path tempDir) {
        SkillManagementDeploymentConfig config = SkillManagementDeploymentConfig.of(
                serviceConfig(tempDir, SkillManagementEventStoreConfig.memory()),
                SkillManagementMaintenanceSourceConfig.none(),
                SkillManagementMaintenancePlan.bootstrap());

        SkillManagementPreflightMatrix matrix =
                SkillManagementPreflightReport.empty().matrix(config);

        assertThat(matrix.ready()).isTrue();
        assertThat(matrix.rows())
                .extracting(SkillManagementPreflightMatrix.Row::path)
                .contains("configuration", "target.definition", "target.artifact", "capability.event-pruning");
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

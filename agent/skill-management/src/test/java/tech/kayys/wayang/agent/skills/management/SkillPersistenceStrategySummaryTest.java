package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class SkillPersistenceStrategySummaryTest {

    @Test
    void summarizesDefaultRuntimePersistenceAsEphemeral() {
        SkillPersistenceStrategySummary summary = SkillManagementServiceConfig.defaults()
                .persistenceStrategy();

        assertThat(summary.kind()).isEqualTo(SkillPersistenceStrategySummary.StrategyKind.EPHEMERAL);
        assertThat(summary.kindLabel()).isEqualTo("ephemeral");
        assertThat(summary.fullyDurable()).isFalse();
        assertThat(summary.hasEphemeralRole()).isTrue();
        assertThat(summary.disabledRoles())
                .extracting(SkillPersistenceStrategySummary.RoleStrategy::roleLabel)
                .containsExactly("event-history");
        assertThat(summary.ephemeralRoles())
                .extracting(SkillPersistenceStrategySummary.RoleStrategy::roleLabel)
                .containsExactly("definition", "lifecycle-state", "artifact");
        assertThat(summary.warnings()).containsExactly(
                "Disabled skill persistence roles: event-history",
                "Ephemeral skill persistence roles: definition, lifecycle-state, artifact");
    }

    @Test
    void summarizesObjectStorageProfileAsDurableObjectStorage() {
        SkillManagementServiceConfig config = SkillManagementServiceProfiles.config(
                SkillManagementServiceProfile.OBJECT_STORAGE,
                SkillManagementServiceProfileOptions.defaults()
                        .withObjectPrefix("prod/skills")
                        .withMaxEvents(25));

        SkillPersistenceStrategySummary summary = SkillPersistenceContractMatrix.from(config)
                .strategy();

        assertThat(summary.kind()).isEqualTo(SkillPersistenceStrategySummary.StrategyKind.OBJECT_STORAGE);
        assertThat(summary.fullyDurable()).isTrue();
        assertThat(summary.hasExternalProvider()).isTrue();
        assertThat(summary.hasDurableFallback()).isFalse();
        assertThat(summary.warnings()).isEmpty();
        assertThat(summary.role(SkillPersistenceContractMatrix.Role.EVENT_HISTORY))
                .get()
                .extracting(SkillPersistenceStrategySummary.RoleStrategy::persistenceClass)
                .isEqualTo(SkillPersistenceContractMatrix.PersistenceClass.OBJECT_STORAGE);
    }

    @Test
    void summarizesHybridObjectStorageWithFileFallback() {
        SkillManagementServiceConfig config = SkillManagementServiceProfiles.config(
                SkillManagementServiceProfile.HYBRID_OBJECT_FILE,
                SkillManagementServiceProfileOptions.defaults()
                        .withObjectPrefix("prod/skills")
                        .withBaseDirectory(Path.of("/var/lib/wayang/skills")));

        SkillPersistenceStrategySummary summary = config.persistenceStrategy();

        assertThat(summary.kind()).isEqualTo(SkillPersistenceStrategySummary.StrategyKind.HYBRID_FALLBACK);
        assertThat(summary.fullyDurable()).isTrue();
        assertThat(summary.hasCompositeProvider()).isTrue();
        assertThat(summary.hasDurableFallback()).isTrue();
        assertThat(summary.warnings()).isEmpty();

        SkillPersistenceStrategySummary.RoleStrategy definition =
                summary.role(SkillPersistenceContractMatrix.Role.DEFINITION).orElseThrow();
        assertThat(definition.kind()).isEqualTo(SkillPersistenceStrategySummary.StrategyKind.HYBRID_FALLBACK);
        assertThat(definition.children())
                .extracting(SkillPersistenceStrategySummary.RoleStrategy::provider)
                .containsExactly("object-storage", "filesystem");
    }

    @Test
    void customProviderWinsOverallStrategyAndKeepsWarningsVisible() {
        SkillManagementServiceConfig config = SkillManagementServiceConfig.of(
                SkillDefinitionStoreConfig.fileSystem(Path.of("/tmp/skills/definitions")),
                SkillLifecycleStateStoreConfig.memory(),
                SkillManagementEventStoreConfig.custom("audit-events"),
                SkillArtifactStoreConfig.jdbc(),
                SkillLifecycleStateReconcileOptions.inspectOnly());

        SkillManagementDeploymentPreflightReport report = new SkillManagementDeploymentPreflightReport(
                SkillManagementDeploymentConfig.of(
                        config,
                        SkillManagementMaintenanceSourceConfig.none(),
                        SkillManagementMaintenancePlan.bootstrap()),
                new SkillManagementPreflightReport(null, null, null, null));
        SkillPersistenceStrategySummary summary = report.persistenceStrategy();

        assertThat(summary.kind()).isEqualTo(SkillPersistenceStrategySummary.StrategyKind.CUSTOM);
        assertThat(summary.hasCustomProvider()).isTrue();
        assertThat(summary.customRoles())
                .extracting(SkillPersistenceStrategySummary.RoleStrategy::roleLabel)
                .containsExactly("event-history");
        assertThat(summary.warnings()).containsExactly(
                "Ephemeral skill persistence roles: lifecycle-state, event-history",
                "Custom skill persistence roles need an externally declared durability contract: event-history");
    }
}

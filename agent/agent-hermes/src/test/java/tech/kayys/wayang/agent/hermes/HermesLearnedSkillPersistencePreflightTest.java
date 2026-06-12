package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class HermesLearnedSkillPersistencePreflightTest {

    @Test
    void reportsFileSystemOnlyTargetReadyWithoutRuntimeResources(@TempDir Path tempDir) {
        HermesLearnedSkillPersistencePreflightReport report = HermesLearnedSkillPersistencePreflight.inspect(
                targetPlan("file-system", "file-system", "file-system"),
                new HermesLearnedSkillPersistenceAdapterResolverOptions(tempDir, null, null),
                Optional.empty(),
                Optional.empty());

        assertThat(report.ready()).isTrue();
        assertThat(report.fileSystemOnly()).isTrue();
        assertThat(report.adapterResolution()).isEqualTo("file-system");
        assertThat(report.missingResources()).isEmpty();
        assertThat(report.toMetadata())
                .containsEntry("adapterResolution", "file-system")
                .containsEntry("fileSystemOnly", true)
                .containsEntry("fileSystemDefinitionDirectory", tempDir.resolve("definitions").toAbsolutePath().normalize().toString())
                .containsEntry("fileSystemArtifactDirectory", tempDir.resolve("artifacts").toAbsolutePath().normalize().toString());
    }

    @Test
    void reportsDatabaseAndObjectStorageTargetReadyWhenResourcesExist(@TempDir Path tempDir) {
        HermesLearnedSkillPersistencePreflightReport report = HermesLearnedSkillPersistencePreflight.inspect(
                targetPlan("database", "rustfs", "file-system"),
                new HermesLearnedSkillPersistenceAdapterResolverOptions(
                        tempDir,
                        null,
                        null,
                        "tenant-a/hermes",
                        null,
                        null,
                        "hermes_defs",
                        "hermes_artifacts",
                        false),
                Optional.of(new InMemoryHermesObjectStorageService()),
                Optional.of(new TestDataSource()));

        assertThat(report.ready()).isTrue();
        assertThat(report.dedicatedServiceSupported()).isTrue();
        assertThat(report.dedicatedServiceAvailable()).isTrue();
        assertThat(report.dataSourceRequired()).isTrue();
        assertThat(report.dataSourceAvailable()).isTrue();
        assertThat(report.objectStorageRequired()).isTrue();
        assertThat(report.objectStorageAvailable()).isTrue();
        assertThat(report.adapterResolution()).isEqualTo("dedicated-skill-management");
        assertThat(report.missingResources()).isEmpty();
        assertThat(report.toMetadata())
                .containsEntry("definitionStorageFamily", "database")
                .containsEntry("artifactStorageFamily", "object-storage")
                .containsEntry("objectStorageArtifactPrefix", "tenant-a/hermes/artifacts")
                .containsEntry("jdbcDefinitionTableName", "hermes_defs")
                .containsEntry("jdbcArtifactTableName", "hermes_artifacts")
                .containsEntry("jdbcInitializeSchema", false);
    }

    @Test
    void reportsMissingRuntimeResourcesForConfiguredDurableTargets(@TempDir Path tempDir) {
        HermesLearnedSkillPersistencePreflightReport report = HermesLearnedSkillPersistencePreflight.inspect(
                targetPlan("database", "s3", "file-system"),
                new HermesLearnedSkillPersistenceAdapterResolverOptions(tempDir, null, null),
                Optional.empty(),
                Optional.empty());

        assertThat(report.ready()).isFalse();
        assertThat(report.dedicatedServiceAvailable()).isFalse();
        assertThat(report.usesProvidedSkillManagement()).isTrue();
        assertThat(report.adapterResolution()).isEqualTo("provided-skill-management");
        assertThat(report.missingResources()).containsExactly("DataSource", "ObjectStorageService");
        assertThat(report.validationIssues())
                .extracting(HermesSkillPersistenceValidationIssue::reason)
                .contains(
                        "Database learned-skill persistence requires a DataSource",
                        "Object-storage learned-skill persistence requires an ObjectStorageService");
        assertThat(report.attention())
                .contains(
                        "Missing learned-skill persistence resources: DataSource, ObjectStorageService",
                        "Configured learned-skill persistence target is unavailable; resolver will use the provided SkillManagementService");
        assertThat(report.toMetadata())
                .containsEntry("ready", false)
                .containsEntry("dataSourceRequired", true)
                .containsEntry("dataSourceAvailable", false)
                .containsEntry("objectStorageRequired", true)
                .containsEntry("objectStorageAvailable", false)
                .containsEntry("validationIssueCount", 2);
    }

    @Test
    void reportsUnsupportedCustomStoreAsValidationIssue(@TempDir Path tempDir) {
        HermesLearnedSkillPersistencePreflightReport report = HermesLearnedSkillPersistencePreflight.inspect(
                targetPlan("custom-vault", "file-system", "file-system"),
                new HermesLearnedSkillPersistenceAdapterResolverOptions(tempDir, null, null),
                Optional.empty(),
                Optional.empty());

        assertThat(report.ready()).isFalse();
        assertThat(report.targetPlanReady()).isTrue();
        assertThat(report.validationIssues()).hasSize(1);
        assertThat(report.validationIssues().get(0))
                .returns(HermesSkillPersistenceRouteRoles.DEFINITIONS, HermesSkillPersistenceValidationIssue::role)
                .returns("custom-vault", HermesSkillPersistenceValidationIssue::store)
                .returns("custom", HermesSkillPersistenceValidationIssue::storeType)
                .returns(
                        "Unsupported learned-skill persistence store 'custom-vault' for definitions",
                        HermesSkillPersistenceValidationIssue::reason);
        assertThat(report.attention())
                .contains("Unsupported learned-skill persistence store 'custom-vault' for definitions");
        assertThat(report.toMetadata())
                .containsEntry("ready", false)
                .containsEntry("validationIssueCount", 1);
    }

    @Test
    void reportsAmbiguousGenericHybridStoreAsValidationIssue(@TempDir Path tempDir) {
        HermesLearnedSkillPersistencePreflightReport report = HermesLearnedSkillPersistencePreflight.inspect(
                targetPlan("hybrid", "file-system", "file-system"),
                new HermesLearnedSkillPersistenceAdapterResolverOptions(tempDir, null, null),
                Optional.empty(),
                Optional.empty());

        assertThat(report.ready()).isFalse();
        assertThat(report.validationIssues())
                .extracting(HermesSkillPersistenceValidationIssue::reason)
                .contains("Ambiguous learned-skill persistence store 'hybrid' for definitions");
    }

    @Test
    void reportsDefaultProvidedSkillManagementModeReady() {
        HermesLearnedSkillPersistencePreflightReport report =
                HermesLearnedSkillPersistencePreflight.inspect(HermesAgentModeConfig.defaults());

        assertThat(report.ready()).isTrue();
        assertThat(report.dedicatedServiceSupported()).isFalse();
        assertThat(report.usesProvidedSkillManagement()).isTrue();
        assertThat(report.adapterResolution()).isEqualTo("provided-skill-management");
        assertThat(report.missingResources()).isEmpty();
        assertThat(report.attention())
                .contains("Learned-skill persistence will use the provided SkillManagementService");
    }

    private static HermesSkillPersistenceTargetPlan targetPlan(
            String definitions,
            String artifacts,
            String fallback) {
        return HermesSkillPersistenceStrategy.fromHints(Map.of(
                HermesSkillPersistenceHintKeys.DEFINITIONS, definitions,
                HermesSkillPersistenceHintKeys.ARTIFACTS, artifacts,
                HermesSkillPersistenceHintKeys.FALLBACK, fallback))
                .routePlan()
                .targetPlan();
    }

    private static final class TestDataSource extends AbstractHermesJdbcDataSource {

        @Override
        protected int executeUpdate(String sql, Map<Integer, Object> parameters) {
            return 0;
        }

        @Override
        protected List<List<Object>> select(String sql, Map<Integer, Object> parameters) {
            return List.of();
        }
    }
}

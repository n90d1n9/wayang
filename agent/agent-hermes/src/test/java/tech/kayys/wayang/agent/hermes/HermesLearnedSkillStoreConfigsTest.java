package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tech.kayys.wayang.agent.skills.management.SkillArtifactStoreConfig;
import tech.kayys.wayang.agent.skills.management.SkillDefinitionStoreConfig;
import tech.kayys.wayang.agent.skills.management.SkillManagementServiceConfig;

import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HermesLearnedSkillStoreConfigsTest {

    @Test
    void translatesDatabaseAndObjectStorageTargetsWithFileFallback(@TempDir Path tempDir) {
        HermesSkillPersistenceTargetPlan targetPlan = HermesSkillPersistenceStrategy.fromHints(Map.of(
                HermesSkillPersistenceHintKeys.DEFINITIONS, "database",
                HermesSkillPersistenceHintKeys.ARTIFACTS, "s3",
                HermesSkillPersistenceHintKeys.FALLBACK, "file-system"))
                .routePlan()
                .targetPlan();
        HermesLearnedSkillPersistenceAdapterResolverOptions options =
                new HermesLearnedSkillPersistenceAdapterResolverOptions(
                        tempDir,
                        null,
                        null,
                        "tenant-a/hermes",
                        null,
                        null,
                        "hermes_skill_defs",
                        "hermes_skill_artifacts",
                        false);

        SkillManagementServiceConfig config =
                HermesLearnedSkillStoreConfigs.serviceConfig(targetPlan, options);

        assertThat(HermesLearnedSkillStoreConfigs.canUseDedicatedSkillManagementService(targetPlan)).isTrue();
        assertThat(HermesLearnedSkillStoreConfigs.requiresDataSource(targetPlan)).isTrue();
        assertThat(HermesLearnedSkillStoreConfigs.requiresObjectStorage(targetPlan)).isTrue();
        assertThat(config.definitionStore().kind()).isEqualTo(SkillDefinitionStoreConfig.Kind.HYBRID);
        assertThat(config.definitionStore().primary().kind()).isEqualTo(SkillDefinitionStoreConfig.Kind.JDBC);
        assertThat(config.definitionStore().primary().jdbcTableName()).isEqualTo("hermes_skill_defs");
        assertThat(config.definitionStore().primary().initializeJdbcSchema()).isFalse();
        assertThat(config.definitionStore().fallback().kind()).isEqualTo(SkillDefinitionStoreConfig.Kind.FILESYSTEM);
        assertThat(config.definitionStore().fallback().directory()).isEqualTo(tempDir.resolve("definitions"));
        assertThat(config.artifactStore().kind()).isEqualTo(SkillArtifactStoreConfig.Kind.HYBRID);
        assertThat(config.artifactStore().primary().kind()).isEqualTo(SkillArtifactStoreConfig.Kind.OBJECT_STORAGE);
        assertThat(config.artifactStore().primary().objectPrefix()).isEqualTo("tenant-a/hermes/artifacts");
        assertThat(config.artifactStore().fallback().kind()).isEqualTo(SkillArtifactStoreConfig.Kind.FILESYSTEM);
        assertThat(config.artifactStore().fallback().directory()).isEqualTo(tempDir.resolve("artifacts"));
    }
}

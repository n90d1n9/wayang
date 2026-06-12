package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HermesSkillPersistenceTargetPlanTest {

    @Test
    void selectsDefaultSkillManagementTargetsWithFileFallback() {
        HermesSkillPersistenceTargetPlan targetPlan =
                HermesSkillPersistencePlan.from(null).targetPlan();

        assertThat(targetPlan.ready()).isTrue();
        assertThat(targetPlan.definitions().selectedBackendId()).contains("skill-management");
        assertThat(targetPlan.artifacts().selectedBackendId()).contains("skill-management");
        assertThat(targetPlan.definitions().fallbackBackendIds()).containsExactly("file-system");
        assertThat(targetPlan.artifacts().fallbackBackendIds()).containsExactly("file-system");
        assertThat(targetPlan.targetSummary())
                .isEqualTo("definitions=skill-management,artifacts=skill-management");
    }

    @Test
    void selectsHybridTargetsByCapability() {
        HermesSkillPersistenceTargetPlan targetPlan = HermesSkillPersistenceStrategy.fromHints(Map.of(
                "definitions", "postgres",
                "artifacts", "rustfs",
                "fallback", "local-file",
                "cloudStores", "s3,minio"))
                .routePlan()
                .targetPlan();

        assertThat(targetPlan.definitions().selectedBackendId()).contains("database");
        assertThat(targetPlan.definitions().credentialed()).isTrue();
        assertThat(targetPlan.artifacts().selectedBackendId()).contains("rustfs");
        assertThat(targetPlan.artifacts().supplementalBackendIds()).containsExactly("s3", "minio");
        assertThat(targetPlan.supplementalCloudBackendIds()).containsExactly("s3", "minio");
        assertThat(targetPlan.fallbackBackendIds()).containsExactly("file-system");
    }

    @Test
    void metadataRendersResolvedTargets() {
        HermesSkillPersistenceTargetPlan targetPlan = HermesSkillPersistenceStrategy.fromHints(Map.of(
                "definitions", "database",
                "artifacts", "s3",
                "fallback", "file-system"))
                .routePlan()
                .targetPlan();

        assertThat(targetPlan.toMetadata())
                .containsEntry("ready", true)
                .containsEntry("targetSummary", "definitions=database,artifacts=s3")
                .containsKey("definitions")
                .containsKey("artifacts");
    }
}

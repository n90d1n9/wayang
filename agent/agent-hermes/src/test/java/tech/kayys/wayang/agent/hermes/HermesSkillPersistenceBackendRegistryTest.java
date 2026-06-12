package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HermesSkillPersistenceBackendRegistryTest {

    @Test
    void buildsRegistryFromPlanRoutes() {
        HermesSkillPersistencePlan plan = HermesSkillPersistenceStrategy.fromHints(Map.of(
                "definitions", "database",
                "artifacts", "rustfs",
                "fallback", "file-system",
                "cloudStores", "s3,minio"))
                .routePlan();

        HermesSkillPersistenceBackendRegistry registry = plan.backendRegistry();

        assertThat(registry.routeCount()).isEqualTo(5);
        assertThat(registry.backendCount()).isEqualTo(5);
        assertThat(registry.backendIds()).containsExactly("database", "rustfs", "s3", "minio", "file-system");
        assertThat(registry.primaryProfiles()).hasSize(4);
        assertThat(registry.credentialedProfiles())
                .extracting(HermesSkillPersistenceBackendProfile::backendId)
                .containsExactly("database", "rustfs", "s3", "minio");
        assertThat(registry.definitionCapableProfiles())
                .extracting(HermesSkillPersistenceBackendProfile::backendId)
                .containsExactly("database", "file-system");
        assertThat(registry.artifactCapableProfiles())
                .extracting(HermesSkillPersistenceBackendProfile::backendId)
                .containsExactly("rustfs", "s3", "minio", "file-system");
        assertThat(registry.fallbackProfiles())
                .extracting(HermesSkillPersistenceBackendProfile::backendId)
                .containsExactly("file-system");
    }

    @Test
    void selectsProfilesByRoleAndStorageFamily() {
        HermesSkillPersistenceBackendRegistry registry = HermesSkillPersistenceStrategy.fromHints(Map.of(
                "definitions", "database",
                "artifacts", "s3",
                "fallback", "local-file"))
                .routePlan()
                .backendRegistry();

        assertThat(registry.profilesByRole("Definitions"))
                .extracting(HermesSkillPersistenceBackendProfile::backendId)
                .containsExactly("database");
        assertThat(registry.profilesByStorageFamily("object-storage"))
                .extracting(HermesSkillPersistenceBackendProfile::backendId)
                .containsExactly("s3");
        assertThat(registry.profilesByStorageFamily("file-system"))
                .extracting(HermesSkillPersistenceBackendProfile::backendId)
                .containsExactly("file-system");
    }

    @Test
    void metadataRendersAdapterFacingProfiles() {
        HermesSkillPersistenceBackendRegistry registry =
                HermesSkillPersistenceBackendRegistry.from(HermesSkillPersistencePlan.from(null));

        assertThat(registry.toMetadata())
                .containsEntry("backendCount", 2)
                .containsEntry("routeCount", 3)
                .containsEntry("readableRouteCount", 3)
                .containsEntry("writableRouteCount", 3)
                .containsEntry("definitionCapableRouteCount", 2)
                .containsEntry("artifactCapableRouteCount", 2)
                .containsKey("targetPlan")
                .containsKey("backendProfiles");
        assertThat(registry.toMetadata().get("backendProfiles"))
                .asList()
                .hasSize(3);
    }
}

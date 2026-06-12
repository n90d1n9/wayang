package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HermesSkillPersistencePlanTest {

    @Test
    void defaultsRouteThroughSkillManagementWithFileFallback() {
        HermesSkillPersistencePlan plan = HermesSkillPersistencePlan.from(HermesSkillPersistenceStrategy.defaults());

        assertThat(plan.routes()).extracting(HermesSkillPersistenceRoute::role)
                .containsExactly(
                        HermesSkillPersistenceRouteRoles.DEFINITIONS,
                        HermesSkillPersistenceRouteRoles.ARTIFACTS,
                        HermesSkillPersistenceRouteRoles.FALLBACK);
        assertThat(plan.routes()).extracting(HermesSkillPersistenceRoute::storeType)
                .containsExactly("skill-management", "skill-management", "file");
        assertThat(plan.fileFallback()).isTrue();
        assertThat(plan.databasePrimary()).isFalse();
        assertThat(plan.cloudBacked()).isFalse();
    }

    @Test
    void hybridDatabaseAndObjectStorageRoutePlanKeepsOrderedFallback() {
        HermesSkillPersistenceStrategy strategy = HermesSkillPersistenceStrategy.fromHints(Map.of(
                "definitions", "database",
                "artifacts", "rustfs",
                "fallback", "file-system",
                "cloudStores", "s3,minio"));

        HermesSkillPersistencePlan plan = strategy.routePlan();

        assertThat(plan.hybrid()).isTrue();
        assertThat(plan.databasePrimary()).isTrue();
        assertThat(plan.cloudBacked()).isTrue();
        assertThat(plan.fileFallback()).isTrue();
        assertThat(plan.cloudStores()).containsExactly("s3", "minio", "rustfs");
        assertThat(plan.routes()).extracting(HermesSkillPersistenceRoute::role)
                .containsExactly(
                        HermesSkillPersistenceRouteRoles.DEFINITIONS,
                        HermesSkillPersistenceRouteRoles.ARTIFACTS,
                        HermesSkillPersistenceRouteRoles.CLOUD,
                        HermesSkillPersistenceRouteRoles.CLOUD,
                        HermesSkillPersistenceRouteRoles.FALLBACK);
        assertThat(plan.routes()).extracting(HermesSkillPersistenceRoute::storeType)
                .containsExactly("database", "object-storage", "object-storage", "object-storage", "file");
        assertThat(plan.routes().getLast().fallback()).isTrue();
    }

    @Test
    void exposesAdapterFriendlyRouteSelectors() {
        HermesSkillPersistencePlan plan = HermesSkillPersistencePlan.from(
                HermesSkillPersistenceStrategy.fromHints(Map.of(
                        "definitions", "database",
                        "artifacts", "rustfs",
                        "fallback", "file-system",
                        "cloudStores", "s3,minio")));

        assertThat(plan.primaryRoutes()).extracting(HermesSkillPersistenceRoute::role)
                .containsExactly("definitions", "artifacts", "cloud", "cloud");
        assertThat(plan.definitionRoutes()).extracting(HermesSkillPersistenceRoute::store)
                .containsExactly("database");
        assertThat(plan.artifactRoutes()).extracting(HermesSkillPersistenceRoute::store)
                .containsExactly("rustfs");
        assertThat(plan.supplementalCloudRoutes()).extracting(HermesSkillPersistenceRoute::store)
                .containsExactly("s3", "minio");
        assertThat(plan.fallbackRoutes()).extracting(HermesSkillPersistenceRoute::store)
                .containsExactly("file-system");
        assertThat(plan.databaseRoutes()).extracting(HermesSkillPersistenceRoute::store)
                .containsExactly("database");
        assertThat(plan.cloudRoutes()).extracting(HermesSkillPersistenceRoute::store)
                .containsExactly("rustfs", "s3", "minio");
        assertThat(plan.fileRoutes()).extracting(HermesSkillPersistenceRoute::store)
                .containsExactly("file-system");
        assertThat(plan.routesByStoreType("object-storage")).extracting(HermesSkillPersistenceRoute::role)
                .containsExactly("artifacts", "cloud", "cloud");
        assertThat(plan.routesByRole("Definitions")).extracting(HermesSkillPersistenceRoute::store)
                .containsExactly("database");
    }

    @Test
    void defaultPlanExposesSkillManagementRoutes() {
        HermesSkillPersistencePlan plan = HermesSkillPersistencePlan.from(HermesSkillPersistenceStrategy.defaults());

        assertThat(plan.skillManagementRoutes()).extracting(HermesSkillPersistenceRoute::role)
                .containsExactly("definitions", "artifacts");
        assertThat(plan.routes().getFirst().toMetadata())
                .containsEntry("skillManagementBacked", true);
    }

    @Test
    void metadataRendersAdapterNeutralRoutes() {
        HermesSkillPersistencePlan plan = HermesSkillPersistencePlan.from(
                HermesSkillPersistenceStrategy.fromHints(Map.of(
                        "definitions", "postgres",
                        "artifacts", "s3",
                        "fallback", "local-file")));

        assertThat(plan.toMetadata())
                .containsEntry("hybrid", true)
                .containsEntry("databasePrimary", true)
                .containsEntry("cloudBacked", true)
                .containsEntry("fileFallback", true);
        assertThat(plan.toMetadata().get("routes")).asList().hasSize(3);
        assertThat(routeMetadata(plan, 0))
                .containsEntry("storeType", "database")
                .containsEntry("databaseBacked", true)
                .containsEntry("cloudBacked", false);
        assertThat(routeMetadata(plan, 1))
                .containsEntry("storeType", "object-storage")
                .containsEntry("cloudBacked", true)
                .containsKey("storeDescriptor");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> routeMetadata(HermesSkillPersistencePlan plan, int index) {
        return (Map<String, Object>) ((java.util.List<?>) plan.toMetadata().get("routes")).get(index);
    }
}

package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HermesSkillPersistenceRoutePlannerTest {

    @Test
    void buildsDefaultRoutesFromNullStrategy() {
        assertThat(HermesSkillPersistenceRoutePlanner.routesFor(null))
                .extracting(HermesSkillPersistenceRoute::role)
                .containsExactly(
                        HermesSkillPersistenceRouteRoles.DEFINITIONS,
                        HermesSkillPersistenceRouteRoles.ARTIFACTS,
                        HermesSkillPersistenceRouteRoles.FALLBACK);
    }

    @Test
    void keepsSupplementalCloudRoutesBeforeFallback() {
        HermesSkillPersistenceStrategy strategy = HermesSkillPersistenceStrategy.fromHints(Map.of(
                "definitions", "database",
                "artifacts", "rustfs",
                "fallback", "file-system",
                "cloudStores", "s3,minio"));

        assertThat(HermesSkillPersistenceRoutePlanner.routesFor(strategy))
                .extracting(HermesSkillPersistenceRoute::store)
                .containsExactly("database", "rustfs", "s3", "minio", "file-system");
        assertThat(HermesSkillPersistenceRoutePlanner.routesFor(strategy))
                .extracting(HermesSkillPersistenceRoute::priority)
                .containsExactly(10, 20, 52, 53, 100);
    }

    @Test
    void skipsCloudStoresAlreadyUsedByPrimaryOrFallbackRoutes() {
        HermesSkillPersistenceStrategy strategy = HermesSkillPersistenceStrategy.fromHints(Map.of(
                "definitions", "database",
                "artifacts", "s3",
                "fallback", "local-file",
                "cloudStores", "s3,rustfs"));

        assertThat(HermesSkillPersistenceRoutePlanner.routesFor(strategy))
                .extracting(HermesSkillPersistenceRoute::role)
                .containsExactly(
                        HermesSkillPersistenceRouteRoles.DEFINITIONS,
                        HermesSkillPersistenceRouteRoles.ARTIFACTS,
                        HermesSkillPersistenceRouteRoles.CLOUD,
                        HermesSkillPersistenceRouteRoles.FALLBACK);
        assertThat(HermesSkillPersistenceRoutePlanner.routesFor(strategy))
                .extracting(HermesSkillPersistenceRoute::store)
                .containsExactly("database", "s3", "rustfs", "local-file");
    }
}

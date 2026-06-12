package tech.kayys.wayang.agent.hermes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Ordered, adapter-neutral routes for learned-skill definitions and artifacts.
 */
public record HermesSkillPersistencePlan(
        List<HermesSkillPersistenceRoute> routes,
        boolean hybrid,
        boolean databasePrimary,
        boolean cloudBacked,
        boolean fileFallback,
        List<String> cloudStores) {

    public HermesSkillPersistencePlan {
        routes = routes == null ? List.of() : List.copyOf(routes);
        cloudStores = cloudStores == null ? List.of() : List.copyOf(cloudStores);
    }

    public static HermesSkillPersistencePlan from(HermesSkillPersistenceStrategy strategy) {
        HermesSkillPersistenceStrategy effective =
                strategy == null ? HermesSkillPersistenceStrategy.defaults() : strategy;
        List<HermesSkillPersistenceRoute> routes = HermesSkillPersistenceRoutePlanner.routesFor(effective);
        boolean databasePrimary = routes.stream()
                .filter(route -> !route.fallback())
                .anyMatch(route -> "database".equals(route.storeType()));
        boolean cloudBacked = routes.stream().anyMatch(HermesSkillPersistenceRoute::cloudBacked);
        return new HermesSkillPersistencePlan(
                routes,
                effective.usesHybridPersistence(),
                databasePrimary,
                cloudBacked,
                effective.hasFileFallback(),
                effective.cloudStores());
    }

    public List<HermesSkillPersistenceRoute> primaryRoutes() {
        return routesMatching(route -> !route.fallback());
    }

    public List<HermesSkillPersistenceRoute> fallbackRoutes() {
        return routesMatching(HermesSkillPersistenceRoute::fallback);
    }

    public List<HermesSkillPersistenceRoute> definitionRoutes() {
        return routesByRole(HermesSkillPersistenceRouteRoles.DEFINITIONS);
    }

    public List<HermesSkillPersistenceRoute> artifactRoutes() {
        return routesByRole(HermesSkillPersistenceRouteRoles.ARTIFACTS);
    }

    public List<HermesSkillPersistenceRoute> supplementalCloudRoutes() {
        return routesByRole(HermesSkillPersistenceRouteRoles.CLOUD);
    }

    public List<HermesSkillPersistenceRoute> databaseRoutes() {
        return routesMatching(HermesSkillPersistenceRoute::databaseBacked);
    }

    public List<HermesSkillPersistenceRoute> cloudRoutes() {
        return routesMatching(HermesSkillPersistenceRoute::cloudBacked);
    }

    public List<HermesSkillPersistenceRoute> fileRoutes() {
        return routesMatching(HermesSkillPersistenceRoute::fileBacked);
    }

    public List<HermesSkillPersistenceRoute> skillManagementRoutes() {
        return routesMatching(HermesSkillPersistenceRoute::skillManagementBacked);
    }

    public List<HermesSkillPersistenceRoute> routesByStoreType(String storeType) {
        String type = HermesText.trimOr(storeType, "");
        return routesMatching(route -> route.storeType().equals(type));
    }

    public List<HermesSkillPersistenceRoute> routesByRole(String role) {
        return routesMatching(route -> route.roleIs(role));
    }

    public HermesSkillPersistenceBackendRegistry backendRegistry() {
        return HermesSkillPersistenceBackendRegistry.from(this);
    }

    public HermesSkillPersistenceTargetPlan targetPlan() {
        return backendRegistry().targetPlan();
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("routes", routes.stream()
                .map(HermesSkillPersistenceRoute::toMetadata)
                .toList());
        metadata.put("hybrid", hybrid);
        metadata.put("databasePrimary", databasePrimary);
        metadata.put("cloudBacked", cloudBacked);
        metadata.put("fileFallback", fileFallback);
        metadata.put("cloudStores", cloudStores);
        metadata.put("backendRegistry", backendRegistry().toMetadata());
        metadata.put("targetPlan", targetPlan().toMetadata());
        return Map.copyOf(metadata);
    }

    private List<HermesSkillPersistenceRoute> routesMatching(
            Predicate<HermesSkillPersistenceRoute> predicate) {
        return routes.stream()
                .filter(predicate)
                .toList();
    }
}

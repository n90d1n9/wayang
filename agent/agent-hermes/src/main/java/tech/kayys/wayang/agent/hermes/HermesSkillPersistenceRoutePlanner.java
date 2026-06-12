package tech.kayys.wayang.agent.hermes;

import java.util.ArrayList;
import java.util.List;

/**
 * Assembles ordered learned-skill persistence routes from strategy intent.
 */
public final class HermesSkillPersistenceRoutePlanner {

    private static final int DEFINITION_PRIORITY = 10;
    private static final int ARTIFACT_PRIORITY = 20;
    private static final int SUPPLEMENTAL_CLOUD_PRIORITY = 50;
    private static final int FALLBACK_PRIORITY = 100;

    private HermesSkillPersistenceRoutePlanner() {
    }

    public static List<HermesSkillPersistenceRoute> routesFor(HermesSkillPersistenceStrategy strategy) {
        HermesSkillPersistenceStrategy effective =
                strategy == null ? HermesSkillPersistenceStrategy.defaults() : strategy;
        List<HermesSkillPersistenceRoute> routes = new ArrayList<>();
        routes.add(route(
                HermesSkillPersistenceRouteRoles.DEFINITIONS,
                effective.definitionStore(),
                DEFINITION_PRIORITY,
                false));
        routes.add(route(
                HermesSkillPersistenceRouteRoles.ARTIFACTS,
                effective.artifactStore(),
                ARTIFACT_PRIORITY,
                false));
        for (String cloudStore : effective.cloudStores()) {
            if (!alreadyRouted(effective, cloudStore)) {
                routes.add(route(
                        HermesSkillPersistenceRouteRoles.CLOUD,
                        cloudStore,
                        SUPPLEMENTAL_CLOUD_PRIORITY + routes.size(),
                        false));
            }
        }
        routes.add(route(
                HermesSkillPersistenceRouteRoles.FALLBACK,
                effective.fallbackStore(),
                FALLBACK_PRIORITY,
                true));
        return List.copyOf(routes);
    }

    private static boolean alreadyRouted(HermesSkillPersistenceStrategy strategy, String store) {
        return HermesSkillPersistenceStoreClassifier.sameStore(store, strategy.definitionStore())
                || HermesSkillPersistenceStoreClassifier.sameStore(store, strategy.artifactStore())
                || HermesSkillPersistenceStoreClassifier.sameStore(store, strategy.fallbackStore());
    }

    private static HermesSkillPersistenceRoute route(
            String role,
            String store,
            int priority,
            boolean fallback) {
        String type = HermesSkillPersistenceStoreClassifier.storeType(store);
        return new HermesSkillPersistenceRoute(
                role,
                store,
                type,
                priority,
                fallback);
    }
}

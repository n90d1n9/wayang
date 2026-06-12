package tech.kayys.wayang.agenticcommerce.wayang;

import java.util.Objects;

/**
 * Context passed to persistence store providers during construction.
 */
public record AgenticCommerceWayangPersistenceProviderContext(
        AgenticCommerceWayangPersistenceConfig config,
        AgenticCommerceObjectStoreClientResolver objectStoreClientResolver,
        AgenticCommerceDatabasePersistenceClientResolver databasePersistenceClientResolver,
        AgenticCommerceWayangPersistenceStoreProviders providers) {

    public AgenticCommerceWayangPersistenceProviderContext {
        config = Objects.requireNonNull(config, "config");
        providers = providers == null
                ? AgenticCommerceWayangPersistenceStoreProviders.defaults()
                : providers;
    }

    public AgenticCommerceWayangPersistenceStore buildNested(AgenticCommerceWayangPersistenceConfig nestedConfig) {
        return providers.build(
                Objects.requireNonNull(nestedConfig, "nestedConfig"),
                objectStoreClientResolver,
                databasePersistenceClientResolver);
    }

    public AgenticCommerceObjectStoreClient resolveObjectStore(AgenticCommerceObjectStoreConfig objectStoreConfig) {
        if (objectStoreClientResolver == null) {
            throw new IllegalArgumentException(
                    "Object-store Agentic Commerce persistence requires an AgenticCommerceObjectStoreClient");
        }
        return objectStoreClientResolver.resolve(Objects.requireNonNull(objectStoreConfig, "objectStoreConfig"));
    }

    public AgenticCommerceDatabasePersistenceClient resolveDatabase(
            AgenticCommerceDatabasePersistenceConfig databaseConfig) {
        if (databasePersistenceClientResolver == null) {
            throw new IllegalArgumentException(
                    "Database Agentic Commerce persistence requires an AgenticCommerceDatabasePersistenceClient");
        }
        return databasePersistenceClientResolver.resolve(Objects.requireNonNull(databaseConfig, "databaseConfig"));
    }
}

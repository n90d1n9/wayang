package tech.kayys.wayang.agenticcommerce.wayang;

import java.util.Objects;

/**
 * Context passed to transfer audit store providers during construction.
 */
public record AgenticCommerceWayangPersistenceTransferAuditProviderContext(
        AgenticCommerceWayangPersistenceTransferAuditConfig config,
        AgenticCommerceObjectStoreClientResolver objectStoreClientResolver,
        AgenticCommerceDatabasePersistenceClientResolver databasePersistenceClientResolver,
        AgenticCommerceWayangPersistenceTransferAuditStoreProviders providers) {

    public AgenticCommerceWayangPersistenceTransferAuditProviderContext {
        config = Objects.requireNonNull(config, "config");
        providers = providers == null
                ? AgenticCommerceWayangPersistenceTransferAuditStoreProviders.defaults()
                : providers;
    }

    public AgenticCommerceWayangPersistenceTransferAuditSink buildNested(
            AgenticCommerceWayangPersistenceTransferAuditConfig nestedConfig) {
        return providers.build(
                Objects.requireNonNull(nestedConfig, "nestedConfig"),
                objectStoreClientResolver,
                databasePersistenceClientResolver);
    }

    public AgenticCommerceObjectStoreClient resolveObjectStore(AgenticCommerceObjectStoreConfig objectStoreConfig) {
        if (objectStoreClientResolver == null) {
            throw new IllegalArgumentException(
                    "Object-store Agentic Commerce persistence transfer audit requires an AgenticCommerceObjectStoreClient");
        }
        return objectStoreClientResolver.resolve(Objects.requireNonNull(objectStoreConfig, "objectStoreConfig"));
    }

    public AgenticCommerceDatabasePersistenceClient resolveDatabase(
            AgenticCommerceDatabasePersistenceConfig databaseConfig) {
        if (databasePersistenceClientResolver == null) {
            throw new IllegalArgumentException(
                    "Database Agentic Commerce persistence transfer audit requires an AgenticCommerceDatabasePersistenceClient");
        }
        return databasePersistenceClientResolver.resolve(Objects.requireNonNull(databaseConfig, "databaseConfig"));
    }
}

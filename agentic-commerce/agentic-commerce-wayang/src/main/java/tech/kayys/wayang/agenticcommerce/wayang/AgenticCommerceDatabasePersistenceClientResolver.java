package tech.kayys.wayang.agenticcommerce.wayang;

/**
 * Resolves database persistence clients for configured database locations.
 */
@FunctionalInterface
public interface AgenticCommerceDatabasePersistenceClientResolver {

    AgenticCommerceDatabasePersistenceClient resolve(AgenticCommerceDatabasePersistenceConfig config);

    static AgenticCommerceDatabasePersistenceClientResolver fixed(AgenticCommerceDatabasePersistenceClient client) {
        java.util.Objects.requireNonNull(client, "client");
        return config -> client;
    }
}

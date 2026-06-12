package tech.kayys.wayang.agenticcommerce.wayang;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Deterministic database persistence client resolver for multi-database stores.
 */
public final class AgenticCommerceDatabasePersistenceClientRegistry
        implements AgenticCommerceDatabasePersistenceClientResolver {

    private final Map<String, AgenticCommerceDatabasePersistenceClient> clients;
    private final AgenticCommerceDatabasePersistenceClient defaultClient;

    private AgenticCommerceDatabasePersistenceClientRegistry(
            Map<String, AgenticCommerceDatabasePersistenceClient> clients,
            AgenticCommerceDatabasePersistenceClient defaultClient) {
        this.clients = Map.copyOf(clients);
        this.defaultClient = defaultClient;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static AgenticCommerceDatabasePersistenceClientRegistry fixed(
            AgenticCommerceDatabasePersistenceClient client) {
        return builder().defaultClient(client).build();
    }

    @Override
    public AgenticCommerceDatabasePersistenceClient resolve(AgenticCommerceDatabasePersistenceConfig config) {
        AgenticCommerceDatabasePersistenceConfig resolved = Objects.requireNonNull(config, "config");
        for (String key : resolutionKeys(resolved)) {
            AgenticCommerceDatabasePersistenceClient client = clients.get(key);
            if (client != null) {
                return client;
            }
        }
        if (defaultClient != null) {
            return defaultClient;
        }
        throw new IllegalArgumentException(
                "No Agentic Commerce database persistence client registered for " + locationLabel(resolved));
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("registeredClientCount", clients.size());
        values.put("defaultClientConfigured", defaultClient != null);
        values.put("registeredKeys", clients.keySet().stream().sorted().toList());
        return Map.copyOf(values);
    }

    private static String[] resolutionKeys(AgenticCommerceDatabasePersistenceConfig config) {
        String provider = normalize(config.provider());
        String tableName = normalize(config.tableName());
        String namespace = normalize(config.namespace());
        return new String[] {
                locationKey(provider, tableName, namespace),
                providerNamespaceKey(provider, namespace),
                tableNamespaceKey(tableName, namespace),
                namespaceKey(namespace),
                tableKey(tableName),
                providerKey(provider)
        };
    }

    private static String locationLabel(AgenticCommerceDatabasePersistenceConfig config) {
        return "provider="
                + config.provider()
                + ", table="
                + config.tableName()
                + ", namespace="
                + config.namespace();
    }

    private static String providerKey(String provider) {
        return "provider:" + normalize(provider);
    }

    private static String tableKey(String tableName) {
        return "table:" + normalize(tableName);
    }

    private static String namespaceKey(String namespace) {
        return "namespace:" + normalize(namespace);
    }

    private static String providerNamespaceKey(String provider, String namespace) {
        return "provider-namespace:" + normalize(provider) + "/" + normalize(namespace);
    }

    private static String tableNamespaceKey(String tableName, String namespace) {
        return "table-namespace:" + normalize(tableName) + "/" + normalize(namespace);
    }

    private static String locationKey(String provider, String tableName, String namespace) {
        return "location:" + normalize(provider) + "/" + normalize(tableName) + "/" + normalize(namespace);
    }

    private static String normalize(String value) {
        return AgenticCommerceWayangMaps.text(value).toLowerCase(Locale.ROOT);
    }

    public static final class Builder {

        private final Map<String, AgenticCommerceDatabasePersistenceClient> clients = new LinkedHashMap<>();
        private AgenticCommerceDatabasePersistenceClient defaultClient;

        private Builder() {
        }

        public Builder defaultClient(AgenticCommerceDatabasePersistenceClient client) {
            this.defaultClient = Objects.requireNonNull(client, "client");
            return this;
        }

        public Builder provider(String provider, AgenticCommerceDatabasePersistenceClient client) {
            return put(providerKey(provider), client);
        }

        public Builder table(String tableName, AgenticCommerceDatabasePersistenceClient client) {
            return put(tableKey(tableName), client);
        }

        public Builder namespace(String namespace, AgenticCommerceDatabasePersistenceClient client) {
            return put(namespaceKey(namespace), client);
        }

        public Builder providerNamespace(
                String provider,
                String namespace,
                AgenticCommerceDatabasePersistenceClient client) {
            return put(providerNamespaceKey(provider, namespace), client);
        }

        public Builder tableNamespace(
                String tableName,
                String namespace,
                AgenticCommerceDatabasePersistenceClient client) {
            return put(tableNamespaceKey(tableName, namespace), client);
        }

        public Builder location(
                AgenticCommerceDatabasePersistenceConfig config,
                AgenticCommerceDatabasePersistenceClient client) {
            AgenticCommerceDatabasePersistenceConfig resolved = Objects.requireNonNull(config, "config");
            return put(locationKey(resolved.provider(), resolved.tableName(), resolved.namespace()), client);
        }

        public AgenticCommerceDatabasePersistenceClientRegistry build() {
            return new AgenticCommerceDatabasePersistenceClientRegistry(clients, defaultClient);
        }

        private Builder put(String key, AgenticCommerceDatabasePersistenceClient client) {
            clients.put(
                    AgenticCommerceWayangMaps.required(key, "database client registry key"),
                    Objects.requireNonNull(client, "client"));
            return this;
        }
    }
}

package tech.kayys.wayang.agenticcommerce.wayang;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Provider-neutral database location for Agentic Commerce persistence documents.
 */
public record AgenticCommerceDatabasePersistenceConfig(
        String provider,
        String tableName,
        String namespace,
        Map<String, Object> attributes) {

    public static final String PROVIDER_DATABASE = "database";
    public static final String PROVIDER_JDBC = "jdbc";
    public static final String PROVIDER_POSTGRES = "postgres";
    public static final String DEFAULT_TABLE = "wayang_agentic_commerce_documents";
    public static final String DEFAULT_NAMESPACE = "agentic-commerce";

    public AgenticCommerceDatabasePersistenceConfig {
        provider = normalizeProvider(provider);
        tableName = normalizeTableName(tableName);
        namespace = normalizeNamespace(namespace);
        attributes = AgenticCommerceWayangMaps.copy(attributes);
    }

    public static AgenticCommerceDatabasePersistenceConfig defaults() {
        return new AgenticCommerceDatabasePersistenceConfig(
                PROVIDER_DATABASE,
                DEFAULT_TABLE,
                DEFAULT_NAMESPACE,
                Map.of());
    }

    public static AgenticCommerceDatabasePersistenceConfig fromMap(Map<?, ?> values) {
        Map<String, Object> resolved = AgenticCommerceWayangMaps.copy(values);
        Map<String, Object> nested = firstMap(
                resolved,
                "database",
                "db",
                "jdbc",
                "postgres",
                "postgresql");
        Map<String, Object> merged = new LinkedHashMap<>(resolved);
        merged.putAll(nested);
        String provider = AgenticCommerceWayangMaps.firstText(
                merged,
                "provider",
                "databaseProvider",
                "storageKind",
                "kind",
                "type",
                "driver");
        String nestedProvider = nestedProvider(resolved);
        if (!nestedProvider.isBlank() && PROVIDER_DATABASE.equals(normalizeProvider(provider))) {
            provider = nestedProvider;
        }
        return new AgenticCommerceDatabasePersistenceConfig(
                provider,
                AgenticCommerceWayangMaps.firstText(merged, "tableName", "table", "documentsTable"),
                AgenticCommerceWayangMaps.firstText(merged, "namespace", "schema", "tenant", "directory", "prefix"),
                firstMap(merged, "attributes", "metadata", "connectionAttributes"));
    }

    public String documentKey(String name) {
        String normalizedName = AgenticCommerceWayangMaps.required(name, "database document name");
        return namespace.isBlank() ? normalizedName : namespace + "/" + normalizedName;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("provider", provider);
        values.put("tableName", tableName);
        values.put("namespace", namespace);
        values.put("attributeCount", attributes.size());
        return Map.copyOf(values);
    }

    public Map<String, Object> toStorageMap() {
        Map<String, Object> values = new LinkedHashMap<>(toMap());
        values.put("attributes", attributes);
        return Map.copyOf(values);
    }

    private static String normalizeProvider(String value) {
        String normalized = AgenticCommerceWayangMaps.text(value).toLowerCase(Locale.ROOT);
        if (normalized.isBlank() || "db".equals(normalized) || "sql".equals(normalized)) {
            return PROVIDER_DATABASE;
        }
        if ("postgresql".equals(normalized) || "pg".equals(normalized)) {
            return PROVIDER_POSTGRES;
        }
        return normalized;
    }

    private static String normalizeTableName(String value) {
        String normalized = AgenticCommerceWayangMaps.text(value);
        return normalized.isBlank() ? DEFAULT_TABLE : normalized;
    }

    private static String normalizeNamespace(String value) {
        String normalized = AgenticCommerceWayangMaps.text(value);
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized.isBlank() ? DEFAULT_NAMESPACE : normalized;
    }

    private static Map<String, Object> firstMap(Map<String, Object> values, String... keys) {
        Object value = AgenticCommerceWayangMaps.first(values, keys);
        return value instanceof Map<?, ?> map ? AgenticCommerceWayangMaps.copy(map) : Map.of();
    }

    private static String nestedProvider(Map<String, Object> values) {
        if (values.containsKey("jdbc")) {
            return PROVIDER_JDBC;
        }
        if (values.containsKey("postgres") || values.containsKey("postgresql")) {
            return PROVIDER_POSTGRES;
        }
        return "";
    }
}

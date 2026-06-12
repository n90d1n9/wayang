package tech.kayys.wayang.agenticcommerce.wayang;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Deterministic database persistence client for tests and embedded runtimes.
 */
public final class InMemoryAgenticCommerceDatabasePersistenceClient
        implements AgenticCommerceDatabasePersistenceClient {

    private final Map<String, StoredDocument> documents = new LinkedHashMap<>();

    public static InMemoryAgenticCommerceDatabasePersistenceClient create() {
        return new InMemoryAgenticCommerceDatabasePersistenceClient();
    }

    @Override
    public synchronized Optional<String> readText(String tableName, String documentKey) {
        return Optional.ofNullable(documents.get(storageKey(tableName, documentKey)))
                .map(StoredDocument::body);
    }

    @Override
    public synchronized void writeText(String tableName, String documentKey, String mimeType, String text) {
        String key = storageKey(tableName, documentKey);
        documents.put(key, new StoredDocument(
                AgenticCommerceWayangMaps.text(tableName),
                AgenticCommerceWayangMaps.text(documentKey),
                AgenticCommerceWayangMaps.text(mimeType),
                text == null ? "" : text));
    }

    @Override
    public synchronized boolean contains(String tableName, String documentKey) {
        return documents.containsKey(storageKey(tableName, documentKey));
    }

    public synchronized Map<String, String> snapshot() {
        Map<String, String> values = new LinkedHashMap<>();
        documents.forEach((key, document) -> values.put(key, document.body()));
        return Map.copyOf(values);
    }

    @Override
    public synchronized Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("clientKind", "in-memory");
        values.put("documentCount", documents.size());
        values.put("tables", documents.values().stream()
                .map(StoredDocument::tableName)
                .distinct()
                .toList());
        return Map.copyOf(values);
    }

    private static String storageKey(String tableName, String documentKey) {
        return AgenticCommerceWayangMaps.required(tableName, "database table name")
                + "::"
                + AgenticCommerceWayangMaps.required(documentKey, "database document key");
    }

    private record StoredDocument(String tableName, String documentKey, String mimeType, String body) {
    }
}

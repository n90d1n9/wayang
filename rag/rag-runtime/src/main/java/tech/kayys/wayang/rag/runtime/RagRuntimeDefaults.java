package tech.kayys.wayang.rag.runtime;

import tech.kayys.wayang.rag.core.RagMetadataKeys;

import java.util.ArrayList;
import java.util.List;

final class RagRuntimeDefaults {

    static final String DEFAULT_NAMESPACE = "default";
    static final String DEFAULT_COLLECTION = DEFAULT_NAMESPACE;
    static final String COLLECTION_METADATA_KEY = RagMetadataKeys.COLLECTION;

    private RagRuntimeDefaults() {
    }

    static String normalizeNamespace(String namespace) {
        return normalizeOrDefault(namespace, DEFAULT_NAMESPACE);
    }

    static String normalizeCollection(String collection) {
        return normalizeOrDefault(collection, DEFAULT_COLLECTION);
    }

    static List<String> defaultCollections() {
        return List.of(DEFAULT_COLLECTION);
    }

    static List<String> normalizeCollections(List<String> collections) {
        if (collections == null || collections.isEmpty()) {
            return List.of();
        }
        List<String> normalized = new ArrayList<>();
        for (String collection : collections) {
            if (collection != null && !collection.isBlank()) {
                normalized.add(collection.trim());
            }
        }
        return RagRuntimeLists.copy(normalized);
    }

    private static String normalizeOrDefault(String value, String defaultValue) {
        return RagRuntimeText.trimToDefault(value, defaultValue);
    }
}

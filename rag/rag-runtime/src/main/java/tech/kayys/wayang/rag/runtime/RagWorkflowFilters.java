package tech.kayys.wayang.rag.runtime;

import tech.kayys.wayang.rag.core.RagMetadataKeys;

import java.util.List;
import java.util.Map;

final class RagWorkflowFilters {

    private RagWorkflowFilters() {
    }

    static List<String> normalizeCollections(List<String> collections) {
        return RagRuntimeDefaults.normalizeCollections(collections);
    }

    static Map<String, Object> copy(Map<String, Object> filters) {
        return RagRuntimeMetadata.copy(filters);
    }

    static Map<String, Object> nativeFilters(Map<String, Object> filters, List<String> collections) {
        Map<String, Object> nativeFilters = RagRuntimeMetadata.mutableCopy(filters);
        normalizeCollections(collections).stream()
                .findFirst()
                .ifPresent(collection -> nativeFilters.put(
                        RagMetadataKeys.COLLECTION,
                        collection));
        return nativeFilters;
    }
}

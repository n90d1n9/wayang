package tech.kayys.wayang.rag.retrieval;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

final class RagRetrievalEvalFilters {

    private RagRetrievalEvalFilters() {
    }

    static Map<String, Object> copy(Map<String, Object> filters) {
        if (filters == null || filters.isEmpty()) {
            return Map.of();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(filters));
    }

    static Map<String, Object> merge(
            Map<String, Object> datasetFilters,
            Map<String, Object> requestFilters,
            Map<String, Object> caseFilters) {
        LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
        putAll(merged, datasetFilters);
        putAll(merged, requestFilters);
        putAll(merged, caseFilters);
        return copy(merged);
    }

    private static void putAll(Map<String, Object> target, Map<String, Object> source) {
        if (source != null && !source.isEmpty()) {
            target.putAll(source);
        }
    }
}

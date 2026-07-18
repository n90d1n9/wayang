package tech.kayys.wayang.agent.spi.skills.rag;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Immutable request passed from the built-in RAG skill to a retriever adapter.
 */
public record RagSkillRetrievalRequest(
        String tenantId,
        String query,
        String collection,
        int topK,
        float[] queryEmbedding,
        Map<String, Object> filters) {

    public RagSkillRetrievalRequest {
        tenantId = trimToEmpty(tenantId);
        query = trimToEmpty(query);
        collection = trimToEmpty(collection);
        topK = Math.max(0, topK);
        queryEmbedding = queryEmbedding == null ? null : queryEmbedding.clone();
        filters = copyFilters(filters);
    }

    @Override
    public float[] queryEmbedding() {
        return queryEmbedding == null ? null : queryEmbedding.clone();
    }

    public boolean hasQueryEmbedding() {
        return queryEmbedding != null && queryEmbedding.length > 0;
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private static Map<String, Object> copyFilters(Map<String, Object> filters) {
        if (filters == null || filters.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> copied = new LinkedHashMap<>();
        filters.forEach((key, value) -> {
            if (key != null && value != null) {
                copied.put(key, value);
            }
        });
        return copied.isEmpty() ? Map.of() : Map.copyOf(copied);
    }
}

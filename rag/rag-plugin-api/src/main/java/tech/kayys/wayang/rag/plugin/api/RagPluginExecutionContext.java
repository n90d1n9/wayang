package tech.kayys.wayang.rag.plugin.api;

import tech.kayys.wayang.rag.core.GenerationConfig;
import java.util.Map;

/**
 * Execution context for RAG plugins.
 */
public record RagPluginExecutionContext(
        String tenantId,
        String query,
        int topK,
        float minSimilarity,
        Map<String, Object> filters,
        GenerationConfig generationConfig,
        boolean retrievalOnly) {

    public RagPluginExecutionContext {
        filters = filters == null ? Map.of() : Map.copyOf(filters);
    }

    public RagPluginExecutionContext withQuery(String updatedQuery) {
        return new RagPluginExecutionContext(
                tenantId,
                updatedQuery,
                topK,
                minSimilarity,
                filters,
                generationConfig,
                retrievalOnly);
    }

    public RagPluginExecutionContext withTopK(int updatedTopK) {
        return new RagPluginExecutionContext(
                tenantId,
                query,
                updatedTopK,
                minSimilarity,
                filters,
                generationConfig,
                retrievalOnly);
    }

    public RagPluginExecutionContext withMinSimilarity(float updatedMinSimilarity) {
        return new RagPluginExecutionContext(
                tenantId,
                query,
                topK,
                updatedMinSimilarity,
                filters,
                generationConfig,
                retrievalOnly);
    }

    public RagPluginExecutionContext withFilters(Map<String, Object> updatedFilters) {
        return new RagPluginExecutionContext(
                tenantId,
                query,
                topK,
                minSimilarity,
                updatedFilters,
                generationConfig,
                retrievalOnly);
    }
}

package tech.kayys.wayang.rag.memory;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.memory.model.Memory;
import tech.kayys.wayang.memory.vector.VectorMemoryAdapter;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * RAG-facing adapter over the active vector memory integration.
 */
public class VectorMemoryRetriever implements RagMemoryRetriever {

    private final VectorMemoryAdapter vectorMemory;

    public VectorMemoryRetriever(VectorMemoryAdapter vectorMemory) {
        this.vectorMemory = Objects.requireNonNull(vectorMemory, "vectorMemory");
    }

    @Override
    public Uni<List<Memory>> retrieve(String query, int topK, Map<String, Object> filters) {
        if (filters == null || filters.isEmpty()) {
            return vectorMemory.searchSimilarMemories(query, topK);
        }
        return vectorMemory.searchMemories(query, topK, filters);
    }
}

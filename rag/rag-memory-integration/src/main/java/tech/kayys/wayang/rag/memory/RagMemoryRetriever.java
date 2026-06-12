package tech.kayys.wayang.rag.memory;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.memory.model.Memory;

import java.util.List;
import java.util.Map;

/**
 * Retrieval boundary for memory systems used by RAG.
 */
public interface RagMemoryRetriever {

    Uni<List<Memory>> retrieve(String query, int topK, Map<String, Object> filters);
}

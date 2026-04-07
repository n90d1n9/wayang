package tech.kayys.wayang.rag.embedding;

import java.util.List;
import java.util.Map;

/**
 * Owned embedding store contract scoped by tenant namespace.
 */
public interface RagEmbeddingStore {

    String add(float[] embedding, String text, Map<String, Object> metadata);

    void add(String id, float[] embedding, String text, Map<String, Object> metadata);

    List<RagEmbeddingMatch> search(float[] queryEmbedding, int topK, double minScore, Map<String, Object> filters);

    void remove(String id);

    void clear();
}

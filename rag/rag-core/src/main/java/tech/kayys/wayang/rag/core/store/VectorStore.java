package tech.kayys.wayang.rag.core.store;

import java.util.List;
import java.util.Map;

public interface VectorStore<T> {

    void upsert(String namespace, String id, float[] vector, T payload, Map<String, Object> metadata);

    List<VectorSearchHit<T>> search(
            String namespace,
            float[] queryVector,
            int topK,
            double minScore,
            Map<String, Object> filters);

    boolean delete(String namespace, String id);

    void clear(String namespace);
}

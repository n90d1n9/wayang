package tech.kayys.wayang.rag.core.store;

import java.util.Map;

public record VectorSearchHit<T>(
        String id,
        T payload,
        double score,
        Map<String, Object> metadata) {
}

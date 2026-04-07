package tech.kayys.wayang.rag.embedding;

import java.util.Map;

public record RagEmbeddingMatch(
        double score,
        String id,
        float[] embedding,
        String text,
        Map<String, Object> metadata) {
}

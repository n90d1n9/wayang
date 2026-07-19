package tech.kayys.wayang.embedding;

import java.util.List;

public record EmbeddingResponse(
        List<float[]> embeddings,
        int dimension,
        String provider,
        String model,
        String version) {

    public EmbeddingResponse(
            List<float[]> embeddings,
            int dimension,
            String provider,
            String model) {
        this(embeddings, dimension, provider, model, "v1");
    }

    public float[] first() {
        if (embeddings == null || embeddings.isEmpty()) {
            throw new EmbeddingException("No embeddings returned");
        }
        return embeddings.get(0);
    }
}
